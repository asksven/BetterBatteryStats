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

int do_request(struct su_initiator *from, struct su_request *to, const char *socket_path)
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
    data.writeString16(String16("android.intent.action.MAIN")); /* action */
    data.writeInt32(NULL_TYPE_ID); /* Uri - type */
    data.writeString16(NULL, 0); /* type */
    data.writeInt32(FLAG_ACTIVITY_NO_HISTORY | FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_MULTIPLE_TASK); /* flags */
    if (sdk_version >= 4) {
        // added in donut
        data.writeString16(String16(REQUESTOR_PACKAGE)); /* package name - DONUT ONLY, NOT IN CUPCAKE. */
    }
    data.writeString16(String16(REQUESTOR_PACKAGE)); /* ComponentName - package */
    data.writeString16(String16(REQUESTOR_PACKAGE "." REQUESTOR_CLASS)); /* ComponentName - class */
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
            data.writeInt32(9); /* writeMapInternal - size */

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
