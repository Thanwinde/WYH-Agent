package com.mvc_client.util;

import org.springframework.core.io.Resource;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.io.InputStream;


public class HashTool {


    public static String getResourceHash(Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return DigestUtils.md5DigestAsHex(inputStream);
        }
    }

}
