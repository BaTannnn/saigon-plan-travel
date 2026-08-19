package com.saigonplantravel.backend.place.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.saigonplantravel.backend.media.MediaStorage;
import com.saigonplantravel.backend.media.MediaUpload;
import com.saigonplantravel.backend.media.StoredMedia;
import com.saigonplantravel.backend.place.exception.InvalidPlaceImageException;
import com.saigonplantravel.backend.place.exception.PlaceImageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class PlaceImageServiceTest {

    @Mock
    private MediaStorage mediaStorage;

    @Mock
    private PlaceImageMetadataService metadataService;

    @Test
    void uploadsFirstCoverImage() {
        MockMultipartFile file = imageFile();
        when(mediaStorage.upload(any(MediaUpload.class)))
                .thenReturn(new StoredMedia("places/demo/new", "https://cdn.example/new.jpg"));
        when(metadataService.replaceCover(
                        "demo-place", new StoredMedia("places/demo/new", "https://cdn.example/new.jpg")))
                .thenReturn(null);

        service().uploadCover("demo-place", file);

        ArgumentCaptor<MediaUpload> upload = ArgumentCaptor.forClass(MediaUpload.class);
        verify(mediaStorage).upload(upload.capture());
        verify(metadataService)
                .replaceCover("demo-place", new StoredMedia("places/demo/new", "https://cdn.example/new.jpg"));
        verify(mediaStorage, never()).delete(any());
        org.assertj.core.api.Assertions.assertThat(upload.getValue().content()).containsExactly(1, 2, 3);
        org.assertj.core.api.Assertions.assertThat(upload.getValue().contentType())
                .isEqualTo("image/jpeg");
        org.assertj.core.api.Assertions.assertThat(upload.getValue().storageKey())
                .startsWith("saigonplantravel/places/demo-place/cover-");
    }

    @Test
    void replacesMetadataBeforeDeletingOldCover() {
        StoredMedia replacement = new StoredMedia("places/demo/new", "https://cdn.example/new.jpg");
        when(mediaStorage.upload(any(MediaUpload.class))).thenReturn(replacement);
        when(metadataService.replaceCover("demo-place", replacement)).thenReturn("places/demo/old");

        service().uploadCover("demo-place", imageFile());

        InOrder order = inOrder(mediaStorage, metadataService);
        order.verify(mediaStorage).upload(any(MediaUpload.class));
        order.verify(metadataService).replaceCover("demo-place", replacement);
        order.verify(mediaStorage).delete("places/demo/old");
    }

    @Test
    void rejectsEmptyUpload() {
        MockMultipartFile empty = new MockMultipartFile("image", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> service().uploadCover("demo-place", empty))
                .isInstanceOf(InvalidPlaceImageException.class)
                .hasMessage("Choose a non-empty image file");
        verifyNoInteractions(mediaStorage, metadataService);
    }

    @Test
    void rejectsNonImageUpload() {
        MockMultipartFile text = new MockMultipartFile("image", "notes.txt", "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> service().uploadCover("demo-place", text))
                .isInstanceOf(InvalidPlaceImageException.class)
                .hasMessage("The uploaded file must have an image MIME type");
        verifyNoInteractions(mediaStorage, metadataService);
    }

    @Test
    void removesCoverMetadataBeforeDeletingAsset() {
        when(metadataService.removeCover("demo-place")).thenReturn("places/demo/old");

        service().removeCover("demo-place");

        InOrder order = inOrder(metadataService, mediaStorage);
        order.verify(metadataService).removeCover("demo-place");
        order.verify(mediaStorage).delete("places/demo/old");
    }

    @Test
    void cleansUpNewUploadWhenMetadataPersistenceFails() {
        StoredMedia uploaded = new StoredMedia("places/demo/new", "https://cdn.example/new.jpg");
        when(mediaStorage.upload(any(MediaUpload.class))).thenReturn(uploaded);
        when(metadataService.replaceCover("demo-place", uploaded)).thenThrow(new IllegalStateException("database"));

        assertThatThrownBy(() -> service().uploadCover("demo-place", imageFile()))
                .isInstanceOf(PlaceImageException.class)
                .hasMessage("Place image metadata could not be saved");
        verify(mediaStorage).delete("places/demo/new");
    }

    private PlaceImageService service() {
        return new PlaceImageService(mediaStorage, metadataService);
    }

    private MockMultipartFile imageFile() {
        return new MockMultipartFile("image", "place.jpg", "image/jpeg", new byte[] {1, 2, 3});
    }
}
