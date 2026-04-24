**Changes**
When a video is playing and the user navigates the seekbar (left/right directional keys), playback now automatically resumes after scrubbing ends. Previously, after seeking while playing, the video would remain paused.

The fix is in `MutablePlayerState.setScrubbing()`: the play state is captured when scrubbing begins, and `play()` is called on the backend when scrubbing ends — only if the video was playing at that time.

**Code assistance**
Code generated with assistance from GitHub Copilot (Claude Sonnet 4.6).

**Issues**
<!-- Fixes # -->
