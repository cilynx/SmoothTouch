# Overview
I'm building SmoothTouch because I've been using perl long enough to believe that easy things should be easy and hard things should be possible.  I've found with many machine interfaces today that easy things are hard, hard things are hard, and cool things are often expensive...and still hard.  My goal is to lower the barrier to entry and make machining simple and easy.  I'm building this app primarily for myself, but if other folks find it useful, that would be just awesome.

![home screen](/output/home_screen.png)

# Features
* 3-axis jog and home
* 3-axis DRO
* 2-level laser safety
 * The button on the home screen sends M3/M5 to arm/disarm the laser (assuming you've wired your laser correctly and aren't just sending PWM into L)
 * The switch in preferences toggles between G0 (safe) and G1 (burn) for jogs
* Simple on/off control for a fan/light (M106S100/M107)
* Halt/Reset (M112/M999) that works even mid-move

# Roadmap
This will mostly be demand-driven through [issues](https://github.com/cilynx/SmoothTouch/issues).
