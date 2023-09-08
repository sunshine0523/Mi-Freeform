ui_print "- Copy Mi-Freeform service dex to /data/system/mi_freeform"

mkdir "/data/system/mi_freeform"
chmod 777 "/data/system/mi_freeform"
cp "$MODPATH/classes.dex" "/data/system/mi_freeform/freeform.dex"