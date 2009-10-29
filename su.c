#define LOG_TAG "su"

#include <sys/types.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/wait.h>
#include <sys/select.h>
#include <unistd.h>
#include <limits.h>
#include <fcntl.h>
#include <errno.h>
#include <endian.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <stdint.h>
#include <pwd.h>

#include <private/android_filesystem_config.h>
#include <cutils/log.h>

#include <sqlite3.h>

extern char* _mktemp(char*); /* mktemp doesn't link right.  Don't ask me why. */

#include "su.h"

static inline char HEX(unsigned char x) {
    x &= 0xf;
    return (x >= 10) ? (x + 'a' - 10) : (x + '0');
}

/* Ewwww.  I'm way too lazy. */
static const char socket_path_template[PATH_MAX] = REQUESTOR_CACHE_PATH "/.socketXXXXXX";
static char socket_path_buf[PATH_MAX];
static char *socket_path = NULL;
static int socket_serv_fd = -1;
static unsigned req_uid = 0;
static unsigned req_gid = 0;

static sqlite3 *db = NULL;

static struct su_initiator su_from = {
    .pid = -1,
    .uid = 0,
    .gid = 0,
    .bin = "",
    .args = "",
};

static struct su_request su_to = {
    .uid = AID_ROOT,
    .gid = AID_ROOT,
    .command = DEFAULT_COMMAND,
};

static int from_init(struct su_initiator *from)
{
    char path[PATH_MAX], exe[PATH_MAX];
    char args[4096], *argv0, *argv_rest;
    int fd;
    ssize_t len;
    int i;

    from->uid = getuid();
    from->gid = getgid();
    from->pid = getppid();

    /* Get the command line */
    snprintf(path, sizeof(path), "/proc/%u/cmdline", from->pid);
    fd = open(path, O_RDONLY);
    if (fd < 0) {
        PLOGE("Opening command line");
        return -1;
    }
    len = read(fd, args, sizeof(args));
    close(fd);
    if (len < 0 || len == sizeof(args)) {
        PLOGE("Reading command line");
        return -1;
    }

    argv0 = args;
    argv_rest = NULL;
    for (i = 0; i < len; i++) {
        if (args[i] == '\0') {
            if (!argv_rest) {
                argv_rest = &args[i+1];
            } else {
                args[i] = ' ';
            }
        }
    }
    args[len] = '\0';

    if (argv_rest) {
        strncpy(from->args, argv_rest, sizeof(from->args));
        from->args[sizeof(from->args)-1] = '\0';
    } else {
        from->args[0] = '\0';
    }

    /* If this isn't app_process, use the real path instead of argv[0] */
    snprintf(path, sizeof(path), "/proc/%u/exe", from->pid);
    len = readlink(path, exe, sizeof(exe));
    if (len < 0) {
        PLOGE("Getting exe path");
        return -1;
    }
    exe[len] = '\0';
    if (strcmp(exe, "/system/bin/app_process")) {
        argv0 = exe;
    }

    strncpy(from->bin, argv0, sizeof(from->bin));
    from->bin[sizeof(from->bin)-1] = '\0';

    return 0;
}

static void socket_cleanup(void)
{
    unlink(socket_path);
}

static void cleanup(void)
{
    socket_cleanup();
    if (db) sqlite3_close(db);
}

static void cleanup_signal(int sig)
{
    socket_cleanup();
    exit(sig);
}

static int socket_create_temp()
{
    int fd, err;

    struct sockaddr_un sun;

    fd = socket(AF_LOCAL, SOCK_STREAM, 0);
    if (fd < 0) {
        PLOGE("socket");
        return -1;
    }

    for (;;) {
        memset(&sun, 0, sizeof(sun));
        sun.sun_family = AF_LOCAL;
        strcpy(socket_path_buf, socket_path_template);
        socket_path = _mktemp(socket_path_buf);
        snprintf(sun.sun_path, sizeof(sun.sun_path), "%s", socket_path);

        if (bind(fd, (struct sockaddr*)&sun, sizeof(sun)) < 0) {
            if (errno != EADDRINUSE) {
                PLOGE("bind");
                return -1;
            }
        } else {
            break;
        }
    }

    if (chmod(sun.sun_path, 0600) < 0) {
        PLOGE("chmod(socket)");
        unlink(sun.sun_path);
        return -1;
    }

    if (chown(sun.sun_path, req_uid, req_gid) < 0) {
        PLOGE("chown(socket)");
        unlink(sun.sun_path);
        return -1;
    }

    if (listen(fd, 1) < 0) {
        PLOGE("listen");
        return -1;
    }

    return fd;
}

static int socket_accept(int serv_fd)
{
    struct timeval tv;
    fd_set fds;
    int fd;

    /* Wait 10 seconds for a connection, then give up. */
    tv.tv_sec = 10;
    tv.tv_usec = 0;
    FD_ZERO(&fds);
    FD_SET(serv_fd, &fds);
    if (select(serv_fd + 1, &fds, NULL, NULL, &tv) < 1) {
        PLOGE("select");
        return -1;
    }

    fd = accept(serv_fd, NULL, NULL);
    if (fd < 0) {
        PLOGE("accept");
        return -1;
    }

    return fd;
}

static int socket_receive_result(int serv_fd, char *result, ssize_t result_len)
{
    ssize_t len;
    
    for (;;) {
        int fd = socket_accept(serv_fd);
        if (fd < 0)
            return -1;

        len = read(fd, result, result_len-1);
        if (len < 0) {
            PLOGE("read(result)");
            return -1;
        }

        if (len > 0)
            break;
    }

    result[len] = '\0';

    return 0;
}

static sqlite3 *database_init()
{
    sqlite3 *db;

    if (mkdir(REQUESTOR_DATABASES_PATH, 0771) >= 0) {
        chown(REQUESTOR_DATABASES_PATH, req_uid, req_gid);
    }

    if (sqlite3_open(REQUESTOR_DATABASE_PATH, &db) != SQLITE_OK) {
        LOGE("Couldn't open database");
        return NULL;
    }

    chmod(REQUESTOR_DATABASE_PATH, 0660);
    chown(REQUESTOR_DATABASE_PATH, req_uid, req_gid);

    if (sqlite3_exec(db,
        "CREATE TABLE IF NOT EXISTS permissions (_id INTEGER, from_uid INTEGER, from_gid INTEGER, exec_uid INTEGER, exec_gid INTEGER, exec_command TEXT, allow_deny TEXT, PRIMARY KEY (_id), UNIQUE (from_uid,from_gid,exec_uid,exec_gid,exec_command));",
        NULL,
        NULL, 
        NULL
    ) != SQLITE_OK) {
        LOGE("Couldn't create table");
        sqlite3_close(db);
        return NULL;
    }

    return db;
}

enum {
    DB_INTERACTIVE,
    DB_DENY,
    DB_ALLOW
};

struct database_check_info {
    int result;
};

static int database_check_callback(void *data, int argc, char **argv, char **name)
{
    struct database_check_info *dci = data;

    if (argc != 1 || strcmp(name[0], "allow_deny") || strcmp(argv[0], "allow")) {
        dci->result = DB_DENY;
        return 0;
    }

    if (dci->result == DB_INTERACTIVE)
        dci->result = DB_ALLOW;

    return 0;
}

static int database_check(sqlite3 *db, struct su_initiator *from, struct su_request *to)
{
    char sql[4096];
    struct database_check_info dci;

    sqlite3_snprintf(
        sizeof(sql), sql,
        "SELECT allow_deny FROM permissions WHERE from_uid=%u AND from_gid=%u AND exec_uid=%u AND exec_gid=%u AND exec_command='%q';",
        (unsigned)from->uid, (unsigned)from->gid, to->uid, to->gid, to->command
    );

    if (strlen(sql) >= sizeof(sql)-1)
        return DB_DENY;

    dci.result = DB_INTERACTIVE;

    if (sqlite3_exec(db, sql, database_check_callback, &dci, NULL) != SQLITE_OK)
        return DB_DENY;

    return dci.result;
}

static int database_insert(sqlite3 *db, struct su_initiator *from, struct su_request *to, const char *allow_deny)
{
    char sql[4096];
    struct database_check_info dci;

    sqlite3_snprintf(
        sizeof(sql), sql,
        "INSERT OR FAIL INTO permissions (from_uid,from_gid,exec_uid,exec_gid,exec_command,allow_deny) VALUES (%u,%u,%u,%u,'%q','%q');",
        (unsigned)from->uid, (unsigned)from->gid, to->uid, to->gid, to->command, allow_deny
    );

    if (strlen(sql) >= sizeof(sql)-1)
        return -1;

    if (sqlite3_exec(db, sql, NULL, NULL, NULL) != SQLITE_OK)
        return -1;

    return 0;
}

static void deny(void)
{
    struct su_initiator *from = &su_from;
    struct su_request *to = &su_to;

    LOGW("request rejected (%u:%u->%u:%u %s)", from->uid, from->gid, to->uid, to->gid, to->command);
    fprintf(stderr, "%s\n", strerror(EACCES));
    exit(-1);
}

static void allow(void)
{
    struct su_initiator *from = &su_from;
    struct su_request *to = &su_to;

    setgroups(0, NULL);
    setresgid(to->gid, to->gid, to->gid);
    setresuid(to->uid, to->uid, to->uid);
    LOGD("%u:%u %s executing %u:%u %s", from->uid, from->gid, from->bin, to->uid, to->gid, to->command);
    if (strcmp(to->command, DEFAULT_COMMAND)) {
        execl("/system/bin/sh", "sh", "-c", to->command, (char*)NULL);
    } else {
        execl("/system/bin/sh", "sh", "-", (char*)NULL);
    }
    PLOGE("exec");
    exit(-1);
}

int main(int argc, char *argv[])
{
    struct stat st;
    char buf[64], *result;
    int i;
    int dballow;

    for (i = 1; i < argc; i++) {
        if (!strcmp(argv[i], "-c")) {
            if (++i < argc) {
                su_to.command = argv[i];
            } else {
                deny();
            }
        } else if (!strcmp(argv[i], "-")) {
            ++i;
            break;
        } else {
            break;
        }
    }
    if (i < argc-1) deny();
    if (i == argc-1) {
        struct passwd *pw;
        pw = getpwnam(argv[i]);
        if (!pw) {
            su_to.uid = su_to.gid = atoi(argv[i]);
        } else {
            su_to.uid = pw->pw_uid;
            su_to.gid = pw->pw_gid;
        }
    }

    from_init(&su_from);

    if (su_from.uid == AID_ROOT)
        allow();

    if (stat(REQUESTOR_DATA_PATH, &st) < 0) {
        PLOGE("stat");
        deny();
    }

#if UNSIGNED_PACKAGE
    if (st.st_uid < AID_APP || st.st_gid != st.st_uid)
#else
    if (st.st_uid != AID_SYSTEM || st.st_gid != st.st_uid)
#endif
    {
        LOGE("Bad uid/gid %d/%d for Superuser Requestor application", (int)st.st_uid, (int)st.st_gid);
        deny();
    }

    req_uid = st.st_uid;
    req_gid = st.st_gid;

    if (from_init(&su_from) < 0) {
        deny();
    }

    if (mkdir(REQUESTOR_CACHE_PATH, 0771) >= 0) {
        chown(REQUESTOR_CACHE_PATH, req_uid, req_gid);
    }

    db = database_init();
    if (!db)
        deny();

    dballow = database_check(db, &su_from, &su_to);
    switch (dballow) {
        case DB_DENY: deny();
        case DB_ALLOW: allow();
        case DB_INTERACTIVE: break;
        default: deny();
    }

    socket_serv_fd = socket_create_temp();
    if (socket_serv_fd < 0)
        deny();

    signal(SIGHUP, cleanup_signal);
    signal(SIGPIPE, cleanup_signal);
    signal(SIGTERM, cleanup_signal);
    signal(SIGABRT, cleanup_signal);
    atexit(cleanup);

    if (do_request(&su_from, &su_to, socket_path) < 0)
        deny();

    if (socket_receive_result(socket_serv_fd, buf, sizeof(buf)) < 0)
        deny();

    close(socket_serv_fd);
    socket_cleanup();

    result = buf;

    if (!strcmp(result, "ALWAYS_DENY")) {
        if (database_insert(db, &su_from, &su_to, "deny") < 0) {
            LOGE("Unable to update database with deny (%u:%u %s->%u:%u %s)", su_from.uid, su_from.gid, su_from.bin, su_to.uid, su_to.gid, su_to.command);
            deny();
        }

        deny();
    } else if (!strcmp(result, "ALWAYS_ALLOW")) {
        if (database_insert(db, &su_from, &su_to, "allow") < 0) {
            LOGE("Unable to update database with allow (%u:%u %s->%u:%u %s)", su_from.uid, su_from.gid, su_from.bin, su_to.uid, su_to.gid, su_to.command);
            deny();
        }

        result = "ALLOW";
        // fall through
    }

    if (!strcmp(result, "DENY")) {
        deny();
    } else if (!strcmp(result, "ALLOW")) {
        allow();
    } else {
        LOGE("unknown response from Superuser Requestor: %s", result);
        deny();
    }

    deny();
    return -1;
}
