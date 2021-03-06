package org.openrepose.filters.versioning.util.http;

import org.openrepose.commons.utils.http.media.MediaType;

// NOTE: This does not belong in util - this is a domain object for versioning only
public interface RequestHeaderInfo {

    MediaType getPreferedMediaRange();

    boolean hasMediaRange(MediaType targetRange);
    
    String getHost();
}
