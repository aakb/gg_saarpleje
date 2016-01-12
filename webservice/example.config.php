<?php
// mailcatcher.me
$smtp = (object)[
  'host' => 'tcp://127.0.0.1',
  'port' => 1025,
];

# // Google smtp
# $smtp = (object)[
#  'host' => 'tls://smtp.gmail.com',
#  'port' => '465',
#  'username' => '', // Some gmail username
#  'password' => '', // https://security.google.com/settings/security/apppasswords
# ];

$proxy = (object)[
  'host' => '127.0.0.1',
  'port' => 10000,
];
