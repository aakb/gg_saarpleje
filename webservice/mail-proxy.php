#!/usr/bin/env php -q
<?php
error_reporting(E_ALL);

/* Allow the script to hang around waiting for connections. */
set_time_limit(0);

/* Turn on implicit output flushing so we see what we're getting
 * as it comes in. */
ob_implicit_flush();

// mailcatcher.me
$smtp = (object)[
  'host' => 'tcp://127.0.0.1',
  'port' => 1025,
];

// Google smtp
$smtp = (object)[
 'host' => 'tls://smtp.gmail.com',
 'port' => '465',
 'auth' => true,
 'username' => '', // Some gmail username
 'password' => '', // https://security.google.com/settings/security/apppasswords
];

if (isset($smtp->username) && empty($smtp->username)) {
  die('No smtp username defined');
}
if (isset($smtp->password) && empty($smtp->password)) {
  die('No smtp password defined');
}

$address = '127.0.0.1';
$port = 10000;

$server = stream_socket_server('tcp://' . $address . ':' . $port, $errno, $errstr);

define('NEWLINE', "\r\n");

function smtpCommand($socket, $command, $expect = 0) {
  file_put_contents('php://stderr', $command . PHP_EOL);
  fwrite($socket, $command . NEWLINE);
  $response = fgets($socket);
  if (intval($response) != $expect) {
    file_put_contents('php://stderr', 'Expected ' . $expect . '; got ' . $response . PHP_EOL);
    exit;
  }
  // https://support.google.com/a/answer/3726730?hl=en
  file_put_contents('php://stderr', $response . PHP_EOL);
  return $response;
}

do {
  echo 'Waiting for connection on ' . $address . ':' . $port . ' …' . PHP_EOL;
  if ($client = @stream_socket_accept($server)) {
    is_dir('streams') || mkdir('streams');
    $bufferName = 'streams/' . strftime('%Y-%m-%dH%H-%M-%Svbm') . uniqid() . '.eml';
    $buffer = fopen($bufferName, 'w');
    echo 'Buffer: ' . $bufferName . PHP_EOL;

    $smtpUrl = $smtp->host . ':' . $smtp->port;
    echo 'Creating mailer ' . $smtpUrl . ' ...' . PHP_EOL;
    $smtpSocket = stream_socket_client($smtpUrl);
    $response = fgets($smtpSocket);
    echo '<- ' . $response . PHP_EOL;

    $response = smtpCommand($smtpSocket, 'HELO ' . preg_replace('@^[a-z]+://@', '', $smtp->host), 250);
    echo '<- ' . $response . PHP_EOL;

    if (!empty($smtp->username)) {
      smtpCommand($smtpSocket, 'AUTH LOGIN', 334);
      smtpCommand($smtpSocket, base64_encode($smtp->username), 334);
      smtpCommand($smtpSocket, base64_encode($smtp->password), 235);
    }

    echo 'Reading data ...' . PHP_EOL . PHP_EOL;

    $sendingData = false;
    while (!feof($client)) {
      $data = fgets($client, 2048);
      file_put_contents('php://stderr', '-> ' . $data);
      fputs($smtpSocket, $data);
      if (!$sendingData) {
        $response = fgets($smtpSocket);
        file_put_contents('php://stderr', '<- ' . $response);
        fputs($client, $response);
      }
      if (strpos($data, "DATA") === 0 && trim($data) == "DATA") {
        $sendingData = true;
      } elseif (strpos($data, ".") === 0 && trim($data) == ".") {
        $sendingData = false;
        $response = fgets($smtpSocket);
        file_put_contents('php://stderr', '<- ' . $response);
        fputs($client, $response);
      }
      fputs($buffer, $data);
    }
    fclose($buffer);
    fclose($smtpSocket);
    echo PHP_EOL . PHP_EOL . 'Done (' . $bufferName . ').' . PHP_EOL . PHP_EOL;
  }
} while (true);

fclose($server);
