#!/usr/bin/env bash

# Try to find Inkscape or ImageMagick's convert
find_converter()
{
	if [ -z "$INKSCAPE" ]
	then
		INKSCAPE=`which inkscape` ||
			INKSCAPE='/Applications/Inkscape.app/Contents/Resources/bin/inkscape'
	fi

	if [ -x "$INKSCAPE" ]
	then
		converter()
		{
			"$INKSCAPE" \
				"$PWD/$1" \
				-e "$PWD/$2" \
				-w $3 \
				-h $3
		}
	elif which convert &>/dev/null
	then
		converter()
		{
			convert \
				-background none \
				"$1" \
				-thumbnail $3 \
				-strip \
				"$2"
		}
	else
		return 1
	fi
}

# Convert SVG files in multiple resolutions to PNG
#
# @param 1 - output path
update()
{
	[ "$1" ] || return 1

	type converter &>/dev/null || find_converter || {
		echo "error: no Inkscape and no ImageMagick convert" >&2
		return 1
	}

	local SVG SIZE NEGATE
	local DPI MULTIPLIER
	local DIR PNG

	while read SVG SIZE NEGATE
	do
		SIZE=${SIZE:-24}

		while read DPI MULTIPLIER
		do
			DIR="$1-$DPI"

			[ -d "$DIR" ] || mkdir -p "$DIR" || {
				echo "error: couldn't create $DIR" >&2
				return $?
			}

			PNG=${SVG##*/}
			PNG="$DIR/${PNG%.*}.png"

			# skip existing up to date files
			[ -r "$PNG" ] && [ -z "`find \
				"$SVG" \
				-type f \
				-newer "$PNG"`" ] && continue

			converter \
				"$SVG" \
				"$PNG" \
				`echo "$SIZE*$MULTIPLIER" |
					bc -l |
					cut -d '.' -f 1`

			if (( $NEGATE ))
			then
				convert "$PNG" -negate "$PNG"
			fi
		done <<EOF
xxxhdpi 4
xxhdpi 3
xhdpi 2
hdpi 1.5
mdpi 1
ldpi .75
EOF
done
}

update ShaderEditor/src/main/res/mipmap << EOF
svg/ic_launcher.svg 48
EOF

update ShaderEditor/src/main/res/drawable << EOF
svg/ic_action_add.svg 24 1
svg/ic_action_add_texture.svg 24
svg/ic_action_cut.svg 24 1
svg/ic_action_delete.svg 24 1
svg/ic_action_duplicate.svg 24 1
svg/ic_action_insert_code.svg 24 1
svg/ic_action_insert_tab.svg 24 1
svg/ic_action_preview.svg 24 1
svg/ic_action_rotate_clockwise.svg 24 1
svg/ic_action_run.svg 24 1
svg/ic_action_save.svg 24 1
svg/ic_action_settings.svg 24 1
svg/ic_action_share.svg 24 1
svg/ic_action_update.svg 24 1
EOF
