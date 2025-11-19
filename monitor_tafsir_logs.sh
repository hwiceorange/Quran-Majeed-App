#!/bin/bash
# Tafsir 日志监控脚本
adb logcat | grep -E "TafsirManager|ActivityTafsir|API_REQUEST|API_RESPONSE"
