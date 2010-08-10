#include <unistd.h>
#include <android_runtime/ActivityManager.h>
#include <binder/IBinder.h>
#include <binder/IServiceManager.h>
#include <binder/Parcel.h>
#include <utils/String8.h>
#include <assert.h>

extern "C" {
#include "su.h"
#include <private/android_filesystem_config.h>
#include <cutils/properties.h>
}

using namespace android;

static const int BROADCAST_INTENT_TRANSACTION = IBinder::FIRST_CALL_TRANSACTION + 13;

static const int NULL_TYPE_ID = 0;

static const int VAL_STRING = 0;
static const int VAL_INTEGER = 1;

static const int START_SUCCESS = 0;

int send_intent(struct su_initiator *from, struct su_request *to, const char *socket_path, int type)
{
    char sdk_version_prop[PROPERTY_VALUE_MAX] = "0";
    property_get("ro.build.version.sdk", sdk_version_prop, "0");

    int sdk_version = atoi(sdk_version_prop); 

    sp<IServiceManager> sm = defaultServiceManager();
    sp<IBinder> am = sm->checkService(String16("activity"));
    assert(am != NULL);

    Parcel data, reply;
    data.writeInterfaceToken(String16("android.app.IActivityManager"));

    data.writeStrongBinder(NULL); /* caller */

    /* intent */
    if (type == 0) {
        data.writeString16(String16("com.noshufou.android.su.REQUEST")); /* action */
    } else {
        data.writeString16(String16("com.noshufou.android.su.NOTIFICATION")); /* action */
    }
    data.writeInt32(NULL_TYPE_ID); /* Uri - data */
    data.writeString16(NULL, 0); /* type */
    data.writeInt32(0); /* flags */
    if (sdk_version >= 4) {
        // added in donut
        data.writeString16(NULL, 0); /* package name - DONUT ONLY, NOT IN CUPCAKE. */
    }
    data.writeString16(NULL, 0); /* ComponentName - package */
    data.writeInt32(0); /* Categories - size */
    if (sdk_version >= 7) {
        // added in eclair rev 7
        data.writeInt32(0);
    }
    { /* Extras */
        data.writeInt32(-1); /* dummy, will hold length */
        int oldPos = data.dataPosition();
        data.writeInt32(0x4C444E42); // 'B' 'N' 'D' 'L'
        { /* writeMapInternal */
            data.writeInt32(4); /* writeMapInternal - size */

            data.writeInt32(VAL_STRING);
            data.writeString16(String16("caller_uid"));
            data.writeInt32(VAL_INTEGER);
            data.writeInt32(from->uid);

            data.writeInt32(VAL_STRING);
            data.writeString16(String16("desired_uid"));
            data.writeInt32(VAL_INTEGER);
            data.writeInt32(to->uid);

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

    data.writeInt32(-1); /* Not sure what this is for, but it prevents a warning */

    data.writeStrongBinder(NULL); /* resultTo */
    data.writeInt32(-1); /* resultCode */
    data.writeString16(NULL, 0); /* resultData */

    data.writeInt32(-1); /* somewhere between these two lines is resultExtras */
    data.writeInt32(-1); /* not sure which line it is, but they both need to be here*/
    
    data.writeString16(String16("com.noshufou.android.su.RESPOND")); /* perm */
    data.writeInt32(0); /* serialized */
    data.writeInt32(0); /* sticky */
    data.writeInt32(-1);
    
    status_t ret = am->transact(BROADCAST_INTENT_TRANSACTION, data, &reply);
    if (ret < START_SUCCESS) return -1;

    return 0;
}
