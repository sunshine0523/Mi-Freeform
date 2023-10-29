#PKGNAME="io.sunshine0523.sidebar"
#LSPDDBPATH="/data/adb/lspd/config/modules_config.db"
#
#prepareSQL(){
#	unzip $ZIPFILE sqlite3 -d $TMPDIR/ > /dev/null
#	chmod +x $TMPDIR/sqlite3
#
#	SQLITEPATH="$TMPDIR/sqlite3"
#}
#
## runSQL "database path" "command" - then you can use $SQLRESULT to read the outcome
#runSQL(){
#	SQLRESULT=$($SQLITEPATH $DBPATH "$CMD")
#}
#
##activate PKGNAME in Lsposed
#activateModuleLSPD()
#{
#	DBPATH=$LSPDDBPATH
#
#	ui_print '- Trying to activate the module in Lsposed...'
#
#	CMD="select mid from modules where module_pkg_name like \"$PKGNAME\";" && runSQL
#	OLDMID=$(echo $SQLRESULT | xargs)
#
#
#	if [ $(($OLDMID+0)) -gt 0 ]; then
#		CMD="select mid from modules where mid = $OLDMID and apk_path like \"$PKGPATH\" and enabled = 1;" && runSQL
#		REALMID=$(echo $SQLRESULT | xargs)
#
#		if [ $(($REALMID+0)) = 0 ]; then
#			CMD="delete from scope where mid = $OLDMID;" && runSQL
#			CMD="delete from modules where mid = $OLDMID;" && runSQL
#		fi
#	fi
#
##some commands may fail. It's OK if they do
#	CMD="insert into modules (\"module_pkg_name\", \"apk_path\", \"enabled\") values (\"$PKGNAME\",\"$PKGPATH\", 1);" && runSQL
#
#	CMD="select mid as ss from modules where module_pkg_name = \"$PKGNAME\";" && runSQL
#
#	NEWMID=$(echo $SQLRESULT | xargs)
#
#	CMD="insert into scope (mid, app_pkg_name, user_id) values ($NEWMID, \"system\",0);" && runSQL
#	CMD="insert into scope (mid, app_pkg_name, user_id) values ($NEWMID, \"$PKGNAME\",0);" && runSQL
#}
#
#prepareSQL
#
#if [ $(ls $LSPDDBPATH) = $LSPDDBPATH ]; then
#	activateModuleLSPD
#	ui_print '  Installation Complete!'
#	ui_print '  Please Reboot your device to activate'
#else
#	ui_print '  Lsposed not found!!'
#	ui_print '  This module will not work without Lsposed'
#	ui_print '  Please:'
#	ui_print '- Insall Lsposed'
#	ui_print '- Reboot'
#	ui_print '- Manually enable PixelXpert in Lsposed'
#	ui_print '- Reboot'
#fi