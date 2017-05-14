package com.choobablue.photobooth.publisher;

import java.io.File;
import java.util.concurrent.Future;

/**
 * Created by Matt McHenry on 8/29/2014.
 */
public interface MediaPublisher {

    Future<?> publishMedia(File file, boolean isVideo);

}
