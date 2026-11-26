# Image regression tests

Open PowerShell in the Android Gradle project with an emulator or device connected.

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.example.code.ProfilePhotoTest'
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.example.code.CampsitePhotoTest'
```

## Entrant profile photos

Codex reused the existing bounded photo encoder for gallery and camera images and moved image persistence to the profile screen's Save callback. A failed image read leaves the current selection in place. The camera, gallery, and removal controls wrap on narrow displays. Codex added the Compose test below.

Prompt summary — “Reuse campsite photo processing in the entrant editor and test that removing a saved photo can be canceled or saved.”

| Test | Steps and expected result |
| --- | --- |
| `ProfilePhotoTest.entrantPhotoRemovalRequiresSave` | Open the editor with a Base64 JPEG and verify it renders. Remove it and cancel. Verify the original is retained and no save was submitted. Reopen the editor, remove the photo, and save. Verify the submitted value is empty and the photo disappears. |
| Manual persistence check | Add a large gallery photo and save. Reopen the profile after relaunch and verify it appears. Replace it using the camera and relaunch again. Remove and cancel, then relaunch and confirm the photo is still there. Remove and save, then relaunch and verify the placeholder. |

## Campsite photos

Codex extracted the existing campsite camera and gallery handling into a shared picker while retaining the image size limits. Codex added the saved-image and fallback rendering test and rechecked the existing encoding tests.

Prompt summary — “Share campsite photo handling with the profile editors and verify photo size, decoding, and fallback rendering.”

| Test | Steps and expected result |
| --- | --- |
| `CampsitePhotoTest.largeGalleryPhotoFitsFirestoreAndDecodesForDisplay` | Import a generated JPEG larger than 1 MiB. Verify the compressed bytes fit within 256 KiB and decode to 1280 pixels on the longest side with the original aspect ratio. |
| `CampsitePhotoTest.cameraPreviewUsesTheSameImageFormat` | Encode a bitmap as a camera preview and verify the saved value decodes. |
| `CampsitePhotoTest.savedPhotoRendersAndInvalidPhotoRestoresDefault` | Capture the default background, apply a magenta JPEG, and verify the rendered pixels change to magenta. Supply invalid image data and verify the default background returns. |
| Manual persistence check | As an owner, create a test campsite with a gallery photo, reopen it, and confirm the photo appears in entrant search and campsite details. Replace it using the camera and check both views again after relaunch. Save a removal and verify both views return to their defaults. |

## Campsite owner profile photos

Codex applied the shared photo picker to the owner editor, moved image persistence to its Save callback, and made the photo controls wrap on narrow displays. Codex added the Compose test below.

Prompt summary — “Use bounded gallery and camera photos for owners and test that canceling removal keeps a saved profile photo.”

| Test | Steps and expected result |
| --- | --- |
| `ProfilePhotoTest.ownerPhotoRemovalRequiresSave` | Open the owner editor with a Base64 JPEG and verify it renders. Remove it and cancel. Verify the original is retained and no save was submitted. Reopen, remove, and save. Verify the submitted value is empty and the photo disappears. |
| Manual persistence check | Add or replace the owner photo, save, and relaunch. Verify the owner photo is restored and a campsite owned by that account still displays its own photo. Repeat removal with Cancel and Save, relaunching after each to check that only Save removes the owner photo. |

The automated cases exercise Android image encoding, Compose rendering, and the profile editors' Save callbacks. The manual cases cover the native gallery and camera UI, account reload, and Firestore persistence.
