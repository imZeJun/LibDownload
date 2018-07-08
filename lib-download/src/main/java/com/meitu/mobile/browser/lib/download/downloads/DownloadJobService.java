package com.meitu.mobile.browser.lib.download.downloads;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.database.ContentObserver;
import android.util.Log;
import android.util.SparseArray;

import static com.meitu.mobile.browser.lib.download.constant.Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI;
import static com.meitu.mobile.browser.lib.download.downloads.Constants.TAG;

public class DownloadJobService extends JobService {
    // @GuardedBy("mActiveThreads")
    private SparseArray<DownloadThread> mActiveThreads = new SparseArray<>();

    @Override
    public void onCreate() {
        super.onCreate();

        // While someone is bound to us, watch for database changes that should
        // trigger notification updates.
        getContentResolver().registerContentObserver(ALL_DOWNLOADS_CONTENT_URI, true, mObserver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContentResolver().unregisterContentObserver(mObserver);
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        final int id = params.getJobId();

        // Spin up thread to handle this download
        final DownloadInfo info = DownloadInfo.queryDownloadInfo(this, id);
        if (info == null) {
            Log.w(TAG, "Odd, no details found for download " + id);
            return false;
        }

        final DownloadThread thread;
        synchronized (mActiveThreads) {
            thread = new DownloadThread(this, params, info);
            mActiveThreads.put(id, thread);
        }
        thread.start();

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        final int id = params.getJobId();

        final DownloadThread thread;
        synchronized (mActiveThreads) {
            thread = mActiveThreads.removeReturnOld(id);
        }
        if (thread != null) {
            // If the thread is still running, ask it to gracefully shutdown,
            // and reschedule ourselves to resume in the future.
            thread.requestShutdown();

            Helpers.scheduleJob(this, DownloadInfo.queryDownloadInfo(this, id));
        }
        return false;
    }

    public void jobFinishedInternal(JobParameters params, boolean needsReschedule) {
        synchronized (mActiveThreads) {
            mActiveThreads.remove(params.getJobId());
        }

        // Update notifications one last time while job is protecting us
        mObserver.onChange(false);

        jobFinished(params, needsReschedule);
    }

    private ContentObserver mObserver = new ContentObserver(Helpers.getAsyncHandler()) {
        @Override
        public void onChange(boolean selfChange) {
            Helpers.getDownloadNotifier(DownloadJobService.this).update();
        }
    };
}
