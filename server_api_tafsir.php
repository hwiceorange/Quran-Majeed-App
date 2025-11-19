<?php
/**
 * Tafsir API 服务器端实现
 * 
 * 用途：为 Quran0 应用提供印尼语等自定义 Tafsir（古兰经注释）
 * 服务器目录：/public_html/quran/apis/tafsirs/
 * API 端点：GET /quran/apis/tafsirs/{slug}/by_ayah/{ayahKey}
 * 
 * 示例：
 * GET https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/1:1
 * 
 * 响应格式（兼容 Quran.com API）：
 * {
 *   "tafsir": {
 *     "resource_id": 999,
 *     "text": "...印尼语注释内容...",
 *     "verse_key": "1:1"
 *   }
 * }
 */

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// 处理 OPTIONS 请求（CORS 预检）
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

// ===================================
// 配置数据库连接
// ===================================
$DB_HOST = 'localhost';      // 数据库主机
$DB_NAME = 'quran_database'; // 数据库名称
$DB_USER = 'root';           // 数据库用户名
$DB_PASS = '';               // 数据库密码
$DB_TABLE = 'tafsir_indonesian'; // 印尼语 Tafsir 表名

// ===================================
// 解析 URL 路径
// ===================================
// 期待路径格式: /quran/apis/tafsirs/{slug}/by_ayah/{ayahKey}
$requestUri = $_SERVER['REQUEST_URI'];
$pattern = '#/quran/apis/tafsirs/([^/]+)/by_ayah/([^/?]+)#';

if (!preg_match($pattern, $requestUri, $matches)) {
    http_response_code(404);
    echo json_encode([
        'error' => 'Invalid API endpoint',
        'message' => 'Expected format: /quran/apis/tafsirs/{slug}/by_ayah/{ayahKey}',
        'received_uri' => $requestUri
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

$slug = $matches[1];     // 例如: id-tafsir-kemenag
$ayahKey = $matches[2];  // 例如: 1:1

// ===================================
// 验证 Tafsir slug
// ===================================
// 支持的 Tafsir slug 列表
$supportedTafsirs = [
    'id-tafsir-kemenag' => [
        'table' => 'tafsir_indonesian',
        'language' => 'id',
        'resource_id' => 999,
        'name' => 'Tafsir Al-Qur\'an Kemenag'
    ],
    // 可以在这里添加更多自定义 Tafsir
    // 'ar-tafsir-custom' => [
    //     'table' => 'tafsir_arabic_custom',
    //     'language' => 'ar',
    //     'resource_id' => 1000,
    //     'name' => 'Custom Arabic Tafsir'
    // ],
];

if (!isset($supportedTafsirs[$slug])) {
    http_response_code(404);
    echo json_encode([
        'error' => 'Tafsir not found',
        'message' => "Tafsir '$slug' is not available on this server. Supported: " . implode(', ', array_keys($supportedTafsirs))
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

$tafsirConfig = $supportedTafsirs[$slug];

// ===================================
// 解析 ayahKey
// ===================================
// 格式: surahId:ayahId (例如: 1:1, 2:255)
if (!preg_match('/^(\d+):(\d+)$/', $ayahKey, $ayahMatches)) {
    http_response_code(400);
    echo json_encode([
        'error' => 'Invalid ayah key',
        'message' => 'Expected format: surahId:ayahId (e.g., 1:1)'
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

$surahId = (int)$ayahMatches[1];
$ayahId = (int)$ayahMatches[2];

// 验证范围
if ($surahId < 1 || $surahId > 114 || $ayahId < 1) {
    http_response_code(400);
    echo json_encode([
        'error' => 'Invalid surah or ayah number',
        'message' => 'Surah must be 1-114, Ayah must be >= 1'
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

// ===================================
// 连接数据库并查询
// ===================================
try {
    $pdo = new PDO(
        "mysql:host=$DB_HOST;dbname=$DB_NAME;charset=utf8mb4",
        $DB_USER,
        $DB_PASS,
        [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES => false,
        ]
    );

    // 查询 Tafsir 内容
    // 字段名可能需要根据实际表结构调整
    $tableName = $tafsirConfig['table'];
    $stmt = $pdo->prepare("
        SELECT text, surah_id, ayat_id 
        FROM `$tableName`
        WHERE surah_id = :surah_id 
          AND ayat_id = :ayat_id 
          AND language = :language
        LIMIT 1
    ");
    
    $stmt->execute([
        'surah_id' => $surahId,
        'ayat_id' => $ayahId,
        'language' => $tafsirConfig['language']
    ]);

    $result = $stmt->fetch();

    if (!$result) {
        http_response_code(404);
        echo json_encode([
            'error' => 'Tafsir not found for this ayah',
            'message' => "No tafsir found for $ayahKey in {$tafsirConfig['name']}"
        ], JSON_UNESCAPED_UNICODE);
        exit;
    }

    // ===================================
    // 返回响应（兼容 Quran.com 格式）
    // ===================================
    http_response_code(200);
    echo json_encode([
        'tafsir' => [
            'resource_id' => $tafsirConfig['resource_id'],
            'text' => $result['text'],
            'verse_key' => $ayahKey
        ]
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode([
        'error' => 'Database error',
        'message' => 'Failed to query tafsir: ' . $e->getMessage()
    ], JSON_UNESCAPED_UNICODE);
    exit;
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'error' => 'Server error',
        'message' => $e->getMessage()
    ], JSON_UNESCAPED_UNICODE);
    exit;
}
?>

