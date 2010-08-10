#ifndef SU_h 
#define SU_h 1

#define REQUESTOR_PACKAGE   "com.noshufou.android.su"
#define REQUESTOR_CLASS     "SuRequest"

#define REQUESTOR_DATA_PATH "/data/data/" REQUESTOR_PACKAGE
#define REQUESTOR_CACHE_PATH REQUESTOR_DATA_PATH "/cache"

#define REQUESTOR_DATABASES_PATH REQUESTOR_DATA_PATH "/databases"
#define REQUESTOR_DATABASE_PATH REQUESTOR_DATABASES_PATH "/permissions.sqlite"

#define DEFAULT_COMMAND "/system/bin/sh"

#define VERSION "2.3-ef"

struct su_initiator {
    pid_t pid;
    unsigned uid;
    char bin[PATH_MAX];
    char args[4096];
};

struct su_request {
    unsigned uid;
    char *command;
};

extern int send_intent(struct su_initiator *from, struct su_request *to, const char *socket_path, int type);

#if 0
#undef LOGE
#define LOGE(fmt,args...) fprintf(stderr, fmt , ## args )
#undef LOGD
#define LOGD(fmt,args...) fprintf(stderr, fmt , ## args )
#undef LOGW
#define LOGW(fmt,args...) fprintf(stderr, fmt , ## args )
#endif

#define PLOGE(fmt,args...) LOGE(fmt " failed with %d: %s" , ## args , errno, strerror(errno))
#define PLOGEV(fmt,err,args...) LOGE(fmt " failed with %d: %s" , ## args , err, strerror(err))

#endif
