#!/usr/bin/env php
<?php
error_reporting(E_ALL);

/* Allow the script to hang around waiting for connections. */
set_time_limit(0);

/* Turn on implicit output flushing so we see what we're getting
 * as it comes in. */
ob_implicit_flush();

if (!file_exists(__DIR__ . '/config.php')) {
  die('Cannot read config.php');
}
include_once(__DIR__ . '/config.php');

if (!isset($smtp)) {
  die('No smtp defined');
}
if (isset($smtp->username) && empty($smtp->username)) {
  die('No smtp username defined');
}
if (isset($smtp->password) && empty($smtp->password)) {
  die('No smtp password defined');
}

if (!isset($proxy)) {
  die('No proxy defined');
}
if (!isset($proxy->host)) {
  die('No proxy host defined');
}
if (!isset($proxy->port)) {
  die('No proxy port');
}

$server = stream_socket_server('tcp://' . $proxy->host . ':' . $proxy->port, $errno, $errstr);

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
  echo 'Waiting for connection on ' . $proxy->host . ':' . $proxy->port . ' …' . PHP_EOL;
  if ($client = @stream_socket_accept($server)) {
    is_dir('streams') || mkdir('streams');
    $bufferName = 'streams/' . strftime('%Y-%m-%dH%H-%M-%Svbm') . uniqid() . '.eml';
    $buffer = fopen($bufferName, 'w');
    echo 'Buffer: ' . $bufferName . PHP_EOL;
    echo 'Name: ' . $peername . PHP_EOL;

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
