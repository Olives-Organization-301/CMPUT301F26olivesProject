# QR image fixtures

`c_2.png` is a generated QR code on a larger background. It encodes `c_2`.
The scanner test reads the packaged PNG and checks that the decoded text matches the filename.

To add a photo or screenshot, name it after its QR payload. The test accepts PNG, JPEG, and WebP.
