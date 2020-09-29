# Go through the .cvsignore and remove all files indicated, except for build.override
for name in $(cat .cvsignore)
do
  if [ "$name" != "build.override" ] && [ -e $name ]; then
    echo "Removing $name"
    rm -fr $name
  fi
done