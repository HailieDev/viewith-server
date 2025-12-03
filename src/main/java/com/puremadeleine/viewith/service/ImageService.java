package com.puremadeleine.viewith.service;

import com.puremadeleine.viewith.domain.image.ImageEntity;
import com.puremadeleine.viewith.domain.image.SourceType;
import com.puremadeleine.viewith.provider.ImageProvider;
import com.puremadeleine.viewith.provider.S3Uploader;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ImageService {

    S3Uploader s3Uploader;
    ImageProvider imageProvider;

    public void saveImages(List<MultipartFile> images, Long sourceId, SourceType sourceType) {
        if (Objects.isNull(images) || images.isEmpty()) return;
        List<ImageEntity> imageEntities = new ArrayList<>();
        for (MultipartFile image : images) {
            String imageUrl = s3Uploader.uploadFile(image);
            ImageEntity imageEntity = ImageEntity.createImage(imageUrl, sourceId, sourceType);
            imageEntities.add(imageEntity);
        }
        imageProvider.saveAll(imageEntities);
    }

    public List<String> getReviewImageUrlList(Long sourceId) {
        return imageProvider.getImageList(sourceId, SourceType.REVIEW)
                .stream().map(ImageEntity::getImageUrl).toList();
    }

    public Map<Long, List<String>> getReviewImageUrlMap(List<Long> sourceIds) {
        return imageProvider.getImageList(sourceIds, SourceType.REVIEW)
                .stream()
                .collect(Collectors.groupingBy(
                        ImageEntity::getSourceId,
                        Collectors.mapping(ImageEntity::getImageUrl, Collectors.toList())
                ));
    }
}
