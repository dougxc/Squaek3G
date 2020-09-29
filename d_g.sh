#
# Alternative launcher for d.sh that starts the builder in debug mode so that a JDPA debugger can attach to it
#
EXTRA_BUILDER_VMFLAGS="$EXTRA_BUILDER_VMFLAGS -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8765"
export EXTRA_BUILDER_VMFLAGS
exec d.sh $*
