package com.meitu.mobile.browser.lib.download.downloads;

import android.content.Context;
import android.drm.DrmManagerClient;

import java.io.File;

public class DownloadDrmHelper {

    /** The MIME type of special DRM files */
    public static final String MIMETYPE_DRM_MESSAGE = "application/vnd.oma.drm.message";

    /** The extensions of special DRM files */
    public static final String EXTENSION_DRM_MESSAGE = ".dm";

    public static final String EXTENSION_INTERNAL_FWDL = ".fl";

    /**
     * Checks if the Media Type needs to be DRM converted
     *
     * @param mimetype Media type of the content
     * @return True if convert is needed else false
     */
    public static boolean isDrmConvertNeeded(String mimetype) {
        return MIMETYPE_DRM_MESSAGE.equals(mimetype);
    }

    /**
     * Modifies the file extension for a DRM Forward Lock file NOTE: This
     * function shouldn't be called if the file shouldn't be DRM converted
     */
    public static String modifyDrmFwLockFileExtension(String filename) {
        if (filename != null) {
            int extensionIndex;
            extensionIndex = filename.lastIndexOf(".");
            if (extensionIndex != -1) {
                filename = filename.substring(0, extensionIndex);
            }
            filename = filename.concat(EXTENSION_INTERNAL_FWDL);
        }
        return filename;
    }

    /**
     * Return the original MIME type of the given file, using the DRM framework
     * if the file is protected content.
     */
    public static String getOriginalMimeType(Context context, File file, String currentMime) {
        final DrmManagerClient client = new DrmManagerClient(context);
        try {
            final String rawFile = file.toString();
            if (client.canHandle(rawFile, null)) {
                return client.getOriginalMimeType(rawFile);
            } else {
                return currentMime;
            }
        } finally {
            client.release();
        }
    }
}
