#!/usr/bin/env bash
set -uo pipefail
if [ "$#" -ne 3 ]; then
    echo "Usage: $0 <spigot.jar> <WormholeXTreme.jar> <label>" >&2
    exit 2
fi
SPIGOT_JAR=$(readlink -f "$1")
PLUGIN_JAR=$(readlink -f "$2")
LABEL="$3"
SMOKE_DIR=${SMOKE_DIR:-$PWD/smoke-server}
STARTUP_TIMEOUT=${STARTUP_TIMEOUT:-600}
SUMMARY_FILE=${SUMMARY_FILE:-/dev/null}
for f in "$SPIGOT_JAR" "$PLUGIN_JAR"; do
    if [ ! -f "$f" ]; then
        echo "Missing file: $f" >&2
        exit 2
    fi
done
rm -rf "$SMOKE_DIR"
mkdir -p "$SMOKE_DIR/plugins"
cd "$SMOKE_DIR" || exit 2
cp "$PLUGIN_JAR" plugins/
echo "eula=true" > eula.txt
cat > server.properties <<'EOF'
online-mode=false
level-type=minecraft\:flat
generate-structures=false
allow-nether=false
spawn-protection=0
view-distance=4
simulation-distance=4
motd=WXT test
EOF
cat > bukkit.yml <<'EOF'
settings:
    allow-end: false
EOF
LOG=server.log
: > "$LOG"
mkfifo console.in
exec 3<>console.in
java -Xms1G -Xmx2G -DIReallyKnowWhatIAmDoingISwear=true -jar "$SPIGOT_JAR" nogui < console.in >> "$LOG" 2>&1 &
SERVER_PID=$!
send() {
    echo ">>> console: $1" >> commands.log
    printf '%s\n' "$1" >&3
}
server_running() {
    kill -0 "$SERVER_PID" 2>/dev/null
}
wait_for_log() {
    local pattern=$1 timeout=$2 waited=0
    while ! grep -Eq "$pattern" "$LOG"; do
        if ! server_running; then
            return 2
        fi
        if [ "$waited" -ge "$timeout" ]; then
            return 1
        fi
        sleep 2
        waited=$((waited + 2))
    done
    return 0
}
STARTED=true
wait_for_log 'Done \([0-9.,]+s\)!' "$STARTUP_TIMEOUT"
case $? in
    1) STARTED=false; echo "Server didnt finish within ${STARTUP_TIMEOUT}s" ;;
    2) STARTED=false; echo "Server exited before startup finished" ;;
esac
if [ "$STARTED" = true ]; then
    echo "Server started, testing commands."
    send "plugins"
    sleep 2
    send "wxstatus"
    sleep 3
    send "wxlist all"
    sleep 2
    send "wxtop"
    sleep 2
    send "wxbuildlist"
    sleep 2
    send "wxreload"
    sleep 10
    send "wxstatus"
    sleep 3
    send "stop"
fi
STOPPED_CLEANLY=false
for _ in $(seq 1 60); do
    if ! server_running; then
        STOPPED_CLEANLY=true
        break
    fi
    sleep 2
done
if ! $STOPPED_CLEANLY; then
    echo "Server took too long to stop. Force quitting..."
    kill "$SERVER_PID" 2>/dev/null
    sleep 5
    kill -9 "$SERVER_PID" 2>/dev/null
fi
wait "$SERVER_PID" 2>/dev/null
exec 3>&-
PROBLEMS=problems.txt
WARNINGS=warnings.txt
MESSAGES=$(mktemp)
MATCHES=$(mktemp)
: > "$WARNINGS"
problem() {
    echo "$1" >> "$MESSAGES"
}
[ "$STARTED" = true ] || problem "Server failed to start"
[ "$STOPPED_CLEANLY" = true ] || problem "Server killed due to stop timeout"
if ! grep -Eq '\[WormholeXTreme\] Enabling WormholeXTreme' "$LOG"; then
    problem "WXT failed to enable"
fi
FAIL_PATTERNS=(
    "Could not load 'plugins/"
    'Unsupported API version'
    'Error occurred while (enabling|disabling) WormholeXTreme'
    'Could not pass event .* to WormholeXTreme'
    'Unhandled exception executing command'
    'for WormholeXTreme .* generated an exception'
    '(ERROR|SEVERE)\]: \[WormholeXTreme\]'
    '^[[:space:]]+at de\.luricos\.bukkit\.WormholeXTreme'
    '(NoSuchMethodError|NoSuchFieldError|NoClassDefFoundError|IncompatibleClassChangeError|AbstractMethodError)'
)
for pattern in "${FAIL_PATTERNS[@]}"; do
    grep -En "$pattern" "$LOG" >> "$MATCHES"
done
grep -En '(WARN|WARNING)\]: \[WormholeXTreme\]' "$LOG" >> "$WARNINGS"
{
    cat "$MESSAGES"
    sort -t: -k1,1n -u "$MATCHES"
} > "$PROBLEMS"
rm -f "$MESSAGES" "$MATCHES"
PROBLEM_COUNT=$(wc -l < "$PROBLEMS")
WARNING_COUNT=$(wc -l < "$WARNINGS")
{
    echo "Spigot $LABEL"
    echo
    if [ "$PROBLEM_COUNT" -eq 0 ]; then
        echo "WXT passed successfully"
    else
        echo "$PROBLEM_COUNT problems found:"
        echo
        echo '```'
        head -n 40 "$PROBLEMS"
        echo '```'
    fi
    if [ "$WARNING_COUNT" -gt 0 ]; then
        echo
        echo "$WARNING_COUNT warnings found:"
        echo
        echo '```'
        head -n 20 "$WARNINGS"
        echo '```'
    fi
    echo
} >> "$SUMMARY_FILE"
if [ "$PROBLEM_COUNT" -eq 0 ]; then
    echo "Spigot $LABEL passed"
    exit 0
fi
echo "Spigot $LABEL failed - $PROBLEM_COUNT problems found:"
head -n 40 "$PROBLEMS"
exit 1