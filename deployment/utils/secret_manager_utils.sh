function fetchSecret() {
  local profile=$1
  local region=$2
  local secretId=$3
  local fetchedSecret

  fetchedSecret=$(
    aws secretsmanager get-secret-value --secret-id "$secretId" --query SecretString \
    --output text --region="$region" --profile "$profile"
  )
  echo "$fetchedSecret"
}

if declare -f "$1" > /dev/null
then
  "$@"
else
  echo "'$1' is not a known function name"
  echo "Supported usages include"
  echo "fetchSecret [profile] [region] [secretId]"
  exit 1
fi
