#!/bin/bash

# Daily Quests Feature Test Script
# Usage: ./test_daily_quests.sh

echo "================================================"
echo "Daily Quests Feature Testing Script"
echo "================================================"
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check device connection
echo "📱 Checking device connection..."
adb devices | grep -w "device" > /dev/null
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Device connected${NC}"
else
    echo -e "${RED}❌ No device found. Please connect a device.${NC}"
    exit 1
fi

echo ""
echo "🔍 Testing Daily Quests Feature..."
echo ""

# Clear logcat
adb logcat -c

# Start app
echo "1️⃣  Starting app..."
adb shell am force-stop com.quran.quranaudio.online
adb shell am start -n com.quran.quranaudio.online/.SplashScreenActivity > /dev/null 2>&1

# Wait for app to start
sleep 5

# Check logs
echo "2️⃣  Checking Daily Quests initialization..."
echo ""

DAILY_QUESTS_LOG=$(adb logcat -d | grep "DailyQuestsManager")

if echo "$DAILY_QUESTS_LOG" | grep -q "User not logged in"; then
    echo -e "${YELLOW}⚠️  User is NOT logged in${NC}"
    echo ""
    echo "╔════════════════════════════════════════════════════════════════╗"
    echo "║  🔐 PLEASE LOGIN TO TEST DAILY QUESTS                         ║"
    echo "╠════════════════════════════════════════════════════════════════╣"
    echo "║  Steps:                                                        ║"
    echo "║  1. Tap the Profile icon (top right)                          ║"
    echo "║  2. Select 'Sign in with Google'                              ║"
    echo "║  3. Complete the Google login                                 ║"
    echo "║  4. Run this script again                                     ║"
    echo "╚════════════════════════════════════════════════════════════════╝"
    echo ""
    exit 0
fi

if echo "$DAILY_QUESTS_LOG" | grep -q "Daily Quests initialized successfully"; then
    echo -e "${GREEN}✅ Daily Quests Manager initialized${NC}"
    
    if echo "$DAILY_QUESTS_LOG" | grep -q "No learning plan found"; then
        echo -e "${YELLOW}📝 No learning plan found - showing Create Card${NC}"
        echo ""
        echo "╔════════════════════════════════════════════════════════════════╗"
        echo "║  ✨ EXPECTED UI: \"Create Your Daily Plan\" Card              ║"
        echo "╠════════════════════════════════════════════════════════════════╣"
        echo "║  You should see a green card on the home screen:             ║"
        echo "║                                                                ║"
        echo "║  [✨ Create Your Daily Plan]                                  ║"
        echo "║    Set daily reading goals and track your progress            ║"
        echo "║    [Get Started]                                              ║"
        echo "║                                                                ║"
        echo "║  → Tap \"Get Started\" to create your learning plan           ║"
        echo "╚════════════════════════════════════════════════════════════════╝"
        
    elif echo "$DAILY_QUESTS_LOG" | grep -q "Learning plan found"; then
        echo -e "${GREEN}✅ Learning plan found - showing Quests Cards${NC}"
        echo ""
        echo "╔════════════════════════════════════════════════════════════════╗"
        echo "║  📊 EXPECTED UI: Streak Card + Today's Quests                ║"
        echo "╠════════════════════════════════════════════════════════════════╣"
        echo "║  You should see two cards on the home screen:                ║"
        echo "║                                                                ║"
        echo "║  1. [📊 Streak Card]                                          ║"
        echo "║     - Current Streak: X Days                                  ║"
        echo "║     - Monthly Goal: X / 31                                    ║"
        echo "║     - Progress Bar                                            ║"
        echo "║                                                                ║"
        echo "║  2. [✅ Today's Quests]                                       ║"
        echo "║     - Quran Reading (Read X pages) [Go]                       ║"
        echo "║     - Tajweed Practice (Practice X min) [Go]                  ║"
        echo "║     - Dhikr (Complete 50 Dhikr) [Go]                          ║"
        echo "╚════════════════════════════════════════════════════════════════╝"
        
        # Check specific stats
        echo ""
        echo "3️⃣  Checking Streak Stats..."
        STREAK_LOG=$(adb logcat -d | grep -E "(StreakStats|currentStreak|monthlyProgress)")
        if [ ! -z "$STREAK_LOG" ]; then
            echo -e "${GREEN}✅ Streak data loaded${NC}"
        else
            echo -e "${YELLOW}⚠️  No streak data found in logs${NC}"
        fi
        
        # Check today's progress
        echo ""
        echo "4️⃣  Checking Today's Progress..."
        PROGRESS_LOG=$(adb logcat -d | grep -E "(DailyProgress|task.*Completed)")
        if [ ! -z "$PROGRESS_LOG" ]; then
            echo -e "${GREEN}✅ Today's progress data loaded${NC}"
        else
            echo -e "${YELLOW}⚠️  No progress data found in logs${NC}"
        fi
    fi
else
    echo -e "${RED}❌ Daily Quests initialization failed${NC}"
    echo ""
    echo "Error logs:"
    adb logcat -d | grep -E "(DailyQuests|ERROR|Exception)" | tail -20
fi

echo ""
echo "================================================"
echo "📋 Full Daily Quests Logs:"
echo "================================================"
echo "$DAILY_QUESTS_LOG"

echo ""
echo "================================================"
echo "Test Complete"
echo "================================================"

