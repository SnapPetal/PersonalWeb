#!/bin/bash
set -e

VIDEOS_DIR="./test-videos"
mkdir -p "$VIDEOS_DIR"

echo "Downloading free skateboard trick videos from Pexels..."

# Note: Pexels requires API key for automated downloads
# These are example URLs - you need to manually download from Pexels website
# Or get a free API key from https://www.pexels.com/api/

echo "Download these videos manually from Pexels (100% free, no attribution):"
echo ""
echo "Ollie:"
echo "https://www.pexels.com/video/person-doing-skateboard-trick-4753648/"
echo ""
echo "Kickflip:"
echo "https://www.pexels.com/video/a-man-doing-a-skateboarding-trick-4753975/"
echo ""
echo "Street skating:"
echo "https://www.pexels.com/video/a-man-skateboarding-on-the-street-4754008/"
echo ""
echo "Ramp tricks:"
echo "https://www.pexels.com/video/skateboarder-skating-on-ramp-7989564/"
echo ""
echo "Multiple tricks:"
echo "https://www.pexels.com/video/man-performing-different-skateboard-tricks-3044191/"
echo ""
echo "Save videos to: $VIDEOS_DIR/"
echo ""
echo "Or use Pexels API (free): https://www.pexels.com/api/"
