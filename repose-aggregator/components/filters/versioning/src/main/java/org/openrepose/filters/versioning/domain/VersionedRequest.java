package org.openrepose.filters.versioning.domain;

import org.openrepose.commons.utils.StringUriUtilities;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.string.JCharSequenceFactory;
import org.openrepose.filters.versioning.config.ServiceVersionMapping;
import org.openrepose.filters.versioning.util.http.HttpRequestInfo;
import org.openrepose.filters.versioning.util.http.UniformResourceInfo;

import static org.openrepose.commons.utils.StringUriUtilities.indexOfUriFragment;

public class VersionedRequest {
   
   private final HttpRequestInfo requestInfo;
   private final ServiceVersionMapping mapping;
   private final String clientAddressedHost;
   
   public VersionedRequest(HttpRequestInfo requestInfo, ServiceVersionMapping mapping) {
      this.requestInfo = requestInfo;
      this.mapping = mapping;
      this.clientAddressedHost = requestInfo.getHost();
   }
   
   public ServiceVersionMapping getMapping() {
      return mapping;
   }
   
   public HttpRequestInfo getRequestInfo() {
      return requestInfo;
   }
   
   public String getHost() {
      return clientAddressedHost;
   }
   
   public boolean isRequestForRoot() {
      return "/".equals(StringUriUtilities.formatUri(requestInfo.getUri()));
   }
   
   public boolean requestBelongsToVersionMapping() {
      final String requestedUri = StringUriUtilities.formatUri(requestInfo.getUri());
      final String versionUri = StringUriUtilities.formatUri(mapping.getId());
      
      return indexOfUriFragment(JCharSequenceFactory.jchars(requestedUri), versionUri) == 0;
   }
   
   public boolean requestMatchesVersionMapping() {
      final String requestedUri = StringUriUtilities.formatUri(requestInfo.getUri());
      
      return requestedUri.equals(StringUriUtilities.formatUri(mapping.getId()));
   }
   
   public String asExternalURL() {
      return requestInfo.getUrl();
   }
   
   public String asInternalURL() {
      return StringUtilities.join(requestInfo.getScheme() + "://", clientAddressedHost, asInternalURI());
   }
   
   public String asInternalURI() {
      return removeVersionPrefix(requestInfo, mapping.getId());
   }
   
   private String removeVersionPrefix(UniformResourceInfo requestInfo, String version) {
      if (requestInfo.getUri().charAt(0) != '/') {
         throw new IllegalArgumentException("Request URI must be a URI with a root reference - i.e. the URI must start with '/'");
      }
      
      final String uri = StringUriUtilities.formatUri(requestInfo.getUri());
      final String formattedVersion = StringUriUtilities.formatUri(version);
      
      if (formattedVersion.length() == 1) {
         return uri;
      }
      
      final String containedVersion = formattedVersion + "/";
      
      final int start = uri.indexOf(containedVersion);
      
      if (start >= 0) {
         return uri.replaceFirst(containedVersion, "/");
      } else if (uri.endsWith(formattedVersion)) {
         return uri.replace(formattedVersion, "/");
      }
     
      return uri;
   }
}
