package ch.bubendorf.locusaddon.gsakdatabase.lova;

/**
 *
 * @param <T>
 * See https://divideandconquer.se/2017/03/29/lova-java-8-functional-interface-for-asynctask-on-android/
 */
public class AsyncTaskResult<T> {
    private T result;
    private Exception exception;

    public AsyncTaskResult(final T result) {
        this.result = result;
    }

    public AsyncTaskResult(final Exception exception) {
        this.exception = exception;
    }

    public T getResult() {
        return result;
    }

    public Exception getException() {
        return exception;
    }
}
