$ErrorActionPreference = 'Stop'
$outputDirectory = Join-Path (Split-Path $PSScriptRoot -Parent) 'app/src/main/res/raw'
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
$outputFile = Join-Path $outputDirectory 'test_tone.wav'
$sampleRate = 16000
$durationSeconds = 120
$samples = $sampleRate * $durationSeconds
$dataSize = $samples * 2
$writer = [System.IO.BinaryWriter]::new([System.IO.File]::Create($outputFile))
try {
    $writer.Write([System.Text.Encoding]::ASCII.GetBytes('RIFF'))
    $writer.Write([int]($dataSize + 36))
    $writer.Write([System.Text.Encoding]::ASCII.GetBytes('WAVEfmt '))
    $writer.Write([int]16)
    $writer.Write([int16]1)
    $writer.Write([int16]1)
    $writer.Write([int]$sampleRate)
    $writer.Write([int]($sampleRate * 2))
    $writer.Write([int16]2)
    $writer.Write([int16]16)
    $writer.Write([System.Text.Encoding]::ASCII.GetBytes('data'))
    $writer.Write([int]$dataSize)
    $notes = @(261.6256, 329.6276, 391.9954, 523.2511, 391.9954, 329.6276)
    for ($index = 0; $index -lt $samples; $index++) {
        $notePosition = ($index % $sampleRate) / [double]$sampleRate
        $frequency = $notes[[int][Math]::Floor($index / $sampleRate) % $notes.Count]
        $envelope = [Math]::Min(1, [Math]::Min($notePosition, 1 - $notePosition) / 0.05)
        $value = 1600 * $envelope * [Math]::Sin(2 * [Math]::PI * $frequency * $notePosition)
        $writer.Write([int16]$value)
    }
} finally {
    $writer.Dispose()
}
Get-FileHash -LiteralPath $outputFile -Algorithm SHA256
