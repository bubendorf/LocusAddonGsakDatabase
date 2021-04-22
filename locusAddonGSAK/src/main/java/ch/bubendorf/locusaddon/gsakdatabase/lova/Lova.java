package ch.bubendorf.locusaddon.gsakdatabase.lova;

import android.os.AsyncTask;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 *
 * @param <Param>
 * @param <Result>
 * https://divideandconquer.se/2017/03/29/lova-java-8-functional-interface-for-asynctask-on-android/
 */
public class Lova<Param, Result> extends AsyncTask<Param, Void, AsyncTaskResult<Result>> {

    private final Function<Param, Result> function;
    private Consumer<Result> consumer;
    private Consumer<Exception> errorHandler;

    public Lova(final Function<Param, Result> function) {
        this.function = function;
    }

    public Lova<Param, Result> onSuccess(final Consumer<Result> consumer) {
        this.consumer = consumer;
        return this;
    }

    public Lova<Param, Result> onError(final Consumer<Exception> errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    @SafeVarargs
    @Override
    protected final AsyncTaskResult<Result> doInBackground(final Param... params) {
        try {
            return new AsyncTaskResult<>(function.apply(params[0]));
        } catch (final Exception e) {
            cancel(false);
            return new AsyncTaskResult<>(e);
        }
    }

    @Override
    protected void onPostExecute(final AsyncTaskResult<Result> asyncTaskResult) {
        super.onPostExecute(asyncTaskResult);
        consumer.accept(asyncTaskResult.getResult());
    }

    @Override
    protected void onCancelled(final AsyncTaskResult<Result> asyncTaskResult) {
        super.onCancelled(asyncTaskResult);
        errorHandler.accept(asyncTaskResult.getException());
    }
}
