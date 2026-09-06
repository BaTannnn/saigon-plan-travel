package com.saigonplantravel.backend.media.cloudinary;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CloudinaryProperties.class)
class CloudinaryMediaStorageConfig {}
