package org.openrepose.services.datastore.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public interface MessageDigestFactory {

   String algorithmName();
   
   MessageDigest newMessageDigest() throws NoSuchAlgorithmException;

}
