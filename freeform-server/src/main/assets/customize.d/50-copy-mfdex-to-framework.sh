ui_print "- Copy Mi-Freeform service dex to /system/framework"

mkdir "$MODPATH/system"
mkdir "$MODPATH/system/framework"
cp "$MODPATH/classes.dex" "$MODPATH/system/framework/freeform.dex"