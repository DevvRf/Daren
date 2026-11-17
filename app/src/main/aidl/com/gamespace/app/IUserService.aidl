package com.gamespace.app;

// Runs inside the process Shizuku grants (shell UID normally, root if the
// device is rooted and the user approved root for Shizuku). Shell UID
// already has WRITE_SECURE_SETTINGS and can read dumpsys output, so most
// of what this app needs doesn't actually require full root once Shizuku
// is set up.
interface IUserService {
    String exec(String command);
    void destroy();
}
