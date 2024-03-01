function verifyCoverage() {
    coberturaReport=$1
    targetCoverage=$2

    coverage=$(awk '/line-rate=/{print $1; exit}' "$coberturaReport" | awk -F'"' '{print $2 * 100}')

    if [[ $coverage -lt $targetCoverage ]]
    then
      echo "Code coverage $coverage% does not meet minimum threshold $targetCoverage%"
      exit 1
    else
      echo "Code coverage $coverage% meets minimum threshold $targetCoverage%"
      exit 0
    fi
}

if declare -f "$1" > /dev/null
then
  "$@"
else
  echo "'$1' is not a known function name"
  echo "Supported usages include"
  echo "verifyCoverage [pathToCobertura] [targetCoverage] "
  exit 1
fi
