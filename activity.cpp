#include <unistd.h>
#include <android_runtime/ActivityManager.h>
#include <utils/IBinder.h>
#include <utils/IServiceManager.h>
#include <utils/Parcel.h>
#include <utils/String8.h>
#include <assert.h>

extern "C" {
#include "su.h"
#include <private/android_filesystem_config.h>
}

using namespace android;

static const int START_ACTIVITY_TRANSACTION = IBinder::FIRST_CALL_TRANSACTION + 2;

static const int FLAG_ACTIVITY_NO_HISTORY = 0x40000000;
static const int FLAG_ACTIVITY_NEW_TASK = 0x10000000;
static const int FLAG_ACTIVITY_MULTIPLE_TASK = 0x08000000;

static const int URI_TYPE_ID = 1;
static const int NULL_TYPE_ID = 0;

static const int VAL_NULL = -1;
static const int VAL_STRING = 0;
static const int VAL_INTEGER = 1;

static const int START_SUCCESS = 0;

static const char *aid_to_string(unsigned aid)
{
    unsigned i;
    static char tmp_string[64];

    if (aid >= AID_APP) {
        snprintf(tmp_string, sizeof(tmp_string), "app_%u", aid);
        return tmp_string;
    }

    for (i = 0; i < android_id_count; i++) {
        if (android_ids[i].aid == aid) {
            return android_ids[i].name;
        }
    }

    snprintf(tmp_string, sizeof(tmp_string), "%u", aid);
    return tmp_string;
}

int do_request(struct su_initiator *from, struct su_request *to, const char *socket_path)
{
    sp<IServiceManager> sm = defaultServiceManager();
    sp<IBinder> am = sm->checkService(String16("activity"));
    assert(am != NULL);

    Parcel data, reply;
    data.writeInterfaceToken(String16("android.app.IActivityManager"));

    data.writeStrongBinder(NULL); /* caller */

    /* intent */
    data.writeString16(String16("android.intent.action.MAIN")); /* action */
    data.writeInt32(NULL_TYPE_ID); /* Uri - type */
    data.writeString16(NULL, 0); /* type */
    data.writeInt32(FLAG_ACTIVITY_NO_HISTORY | FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_MULTIPLE_TASK); /* flags */
#if DONUT
    data.writeString16(String16(REQUESTOR_PACKAGE)); /* package name - DONUT ONLY, NOT IN CUPCAKE. */
#endif
    data.writeString16(String16(REQUESTOR_PACKAGE)); /* ComponentName - package */
    data.writeString16(String16(REQUESTOR_PACKAGE "." REQUESTOR_CLASS)); /* ComponentName - class */
    data.writeInt32(0); /* Categories - size */
    { /* Extras */
        data.writeInt32(-1); /* dummy, will hold length */
        int oldPos = data.dataPosition();
        data.writeInt32(0x4C444E42); // 'B' 'N' 'D' 'L'
        { /* writeMapInternal */
            data.writeInt32(9); /* writeMapInternal - size */

            data.writeInt32(VAL_STRING);
            data.writeString16(String16("caller_pid"));
            data.writeInt32(VAL_INTEGER);
            data.writeInt32(from->pid);

            data.writeInt32(VAL_STRING);
            data.writeString16(String16("caller_uid"));
            data.writeInt32(VAL_STRING);
            data.writeString16(String16(aid_to_string(from->uid)));

            data.writeInt32(VAL_STRING);
            data.writeString16(String16("caller_gid"));
            data.writeInt32(VAL_STRING);
            data.writeString16(String16(aid_to_string(from->gid)));

            data.writeInt32(VAL_STRING);
            data.writeString16(String16("caller_bin"));
            data.writeInt32(VAL_STRING);
            data.writeString16(String16(from->bin));

            data.writeInt32(VAL_STRING);
            data.writeString16(String16("caller_args"));
            data.writeInt32(VAL_STRING);
            data.writeString16(String16(from->args));

            data.writeInt32(VAL_STRING);
            data.writeString16(String16("desired_uid"));
            data.writeInt32(VAL_STRING);
            data.writeString16(String16(aid_to_string(to->uid)));

            data.writeInt32(VAL_STRING);
            data.writeString16(String16("desired_gid"));
            data.writeInt32(VAL_STRING);
            data.writeString16(String16(aid_to_string(to->gid)));

            data.writeInt32(VAL_STRING);
            data.writeString16(String16("desired_cmd"));
            data.writeInt32(VAL_STRING);
            data.writeString16(String16(to->command));

            data.writeInt32(VAL_STRING);
            data.writeString16(String16("socket"));
            data.writeInt32(VAL_STRING);
            data.writeString16(String16(socket_path));
        }
        int newPos = data.dataPosition();
        data.setDataPosition(oldPos - 4);
        data.writeInt32(newPos - oldPos); /* length */
        data.setDataPosition(newPos);
    }

    data.writeString16(NULL, 0); /* resolvedType */

    data.writeInt32(-1); /* grantedUriPermissions */
    data.writeInt32(0); /* grantedMode */

    data.writeStrongBinder(NULL); /* resultTo */
    data.writeString16(NULL, 0); /* resultWho */
    data.writeInt32(-1); /* requestCode */
    data.writeInt32(false); /* onlyIfNeeded */
    data.writeInt32(false); /* debug */

    status_t ret = am->transact(START_ACTIVITY_TRANSACTION, data, &reply);
    if (ret < START_SUCCESS) return -1;

    return 0;
}
