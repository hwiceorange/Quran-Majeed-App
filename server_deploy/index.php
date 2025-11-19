<?php
/**
 * Tafsir API 服务器端实现
 * 
 * 部署位置：/public_html/quran/apis/tafsirs/index.php
 * API 端点：GET https://apis.dochubai.com/quran/apis/tafsirs/{slug}/by_ayah/{ayahKey}
 * 
 * 示例：
 * GET https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/1:1
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
// 数据库配置
// ===================================
$DB_HOST = '153.92.15.3';              // 数据库主机
$DB_NAME = 'u853729749_quran_database'; // 数据库名称
$DB_USER = 'u853729749_IceOrange';     // 数据库用户名
$DB_PASS = 'Hcbzln123#';               // 数据库密码

// ===================================
// 支持的 Tafsir 配置
// ===================================
$supportedTafsirs = [
    'id-tafsir-kemenag' => [
        'table' => 'tafsir_indonesian',
        'language' => 'id',
        'resource_id' => 999,
        'name' => 'Tafsir Al-Qur\'an Kemenag'
    ],
];

// ===================================
// 解析 URL 路径（支持两种方式）
// ===================================

// 方式 1: 通过 GET 参数（不需要 .htaccess）
// 示例: index.php?slug=id-tafsir-kemenag&ayah=1:1
if (isset($_GET['slug']) && isset($_GET['ayah'])) {
    $slug = $_GET['slug'];
    $ayahKey = $_GET['ayah'];
} 
// 方式 2: 通过 URL 重写（需要 .htaccess）
// 示例: /quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/1:1
else {
    $requestUri = $_SERVER['REQUEST_URI'];
    
    // 移除查询字符串
    $requestUri = strtok($requestUri, '?');
    
    // 期待路径格式: /quran/apis/tafsirs/{slug}/by_ayah/{ayahKey}
    $pattern = '#/quran/apis/tafsirs/([^/]+)/by_ayah/([^/?]+)#';
    
    if (!preg_match($pattern, $requestUri, $matches)) {
        http_response_code(404);
        echo json_encode([
            'error' => 'Invalid API endpoint',
            'message' => 'Please use one of the following formats',
            'formats' => [
                'url_rewrite' => '/quran/apis/tafsirs/{slug}/by_ayah/{ayahKey}',
                'direct' => '/quran/apis/tafsirs/index.php?slug={slug}&ayah={ayahKey}'
            ],
            'examples' => [
                'url_rewrite' => 'https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/1:1',
                'direct' => 'https://apis.dochubai.com/quran/apis/tafsirs/index.php?slug=id-tafsir-kemenag&ayah=1:1'
            ],
            'received_uri' => $requestUri
        ], JSON_UNESCAPED_UNICODE);
        exit;
    }
    
    $slug = $matches[1];     // 例如: id-tafsir-kemenag
    $ayahKey = $matches[2];  // 例如: 1:1
}

// ===================================
// 验证 Tafsir slug
// ===================================
if (!isset($supportedTafsirs[$slug])) {
    http_response_code(404);
    echo json_encode([
        'error' => 'Tafsir not found',
        'message' => "Tafsir '$slug' is not available on this server.",
        'supported_tafsirs' => array_keys($supportedTafsirs)
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

$tafsirConfig = $supportedTafsirs[$slug];

// ===================================
// 解析 ayahKey
// ===================================
if (!preg_match('/^(\d+):(\d+)$/', $ayahKey, $ayahMatches)) {
    http_response_code(400);
    echo json_encode([
        'error' => 'Invalid ayah key',
        'message' => 'Expected format: surahId:ayahId (e.g., 1:1)',
        'received' => $ayahKey
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
        'message' => 'Surah must be 1-114, Ayah must be >= 1',
        'received' => ['surah' => $surahId, 'ayah' => $ayahId]
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
            'message' => "No tafsir found for $ayahKey in {$tafsirConfig['name']}",
            'debug' => [
                'surah_id' => $surahId,
                'ayat_id' => $ayahId,
                'language' => $tafsirConfig['language'],
                'table' => $tableName
            ]
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
    ], JSON_UNESCAPED_UNICODE);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode([
        'error' => 'Database error',
        'message' => 'Failed to connect or query database',
        'debug' => [
            'db_host' => $DB_HOST,
            'db_name' => $DB_NAME,
            'error_message' => $e->getMessage()
        ]
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

