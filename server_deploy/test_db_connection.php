<?php
/**
 * 数据库连接测试脚本
 * 
 * 部署位置：/public_html/quran/apis/tafsirs/test_db_connection.php
 * 访问：https://apis.dochubai.com/quran/apis/tafsirs/test_db_connection.php
 * 
 * ⚠️ 测试完成后请删除此文件，避免泄露数据库信息！
 */

header('Content-Type: application/json; charset=utf-8');

// ===================================
// 📝 请修改为您的实际数据库配置
// ===================================
$DB_HOST = '153.92.15.3';      // 数据库主机
$DB_NAME = 'u853729749_quran_database'; // 数据库名称
$DB_USER = 'u853729749_IceOrange';           // 数据库用户名
$DB_PASS = 'Hcbzln123#';               // 数据库密码
$DB_TABLE = 'tafsir_indonesian'; // 印尼语 Tafsir 表名
$results = [
    'status' => 'testing',
    'tests' => []
];

// ===================================
// 测试 1: PHP 版本
// ===================================
$results['tests'][] = [
    'test' => 'PHP Version',
    'status' => 'success',
    'result' => PHP_VERSION,
    'message' => 'PHP version: ' . PHP_VERSION
];

// ===================================
// 测试 2: PDO 扩展
// ===================================
if (extension_loaded('pdo') && extension_loaded('pdo_mysql')) {
    $results['tests'][] = [
        'test' => 'PDO Extension',
        'status' => 'success',
        'message' => 'PDO and PDO_MYSQL extensions are available'
    ];
} else {
    $results['tests'][] = [
        'test' => 'PDO Extension',
        'status' => 'error',
        'message' => 'PDO or PDO_MYSQL extension not available'
    ];
    $results['status'] = 'error';
}

// ===================================
// 测试 3: 数据库连接
// ===================================
try {
    $pdo = new PDO(
        "mysql:host=$DB_HOST;dbname=$DB_NAME;charset=utf8mb4",
        $DB_USER,
        $DB_PASS,
        [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        ]
    );
    
    $results['tests'][] = [
        'test' => 'Database Connection',
        'status' => 'success',
        'message' => 'Successfully connected to database'
    ];
    
    // ===================================
    // 测试 4: 检查表是否存在
    // ===================================
    $stmt = $pdo->query("SHOW TABLES LIKE 'tafsir_indonesian'");
    $tableExists = $stmt->rowCount() > 0;
    
    if ($tableExists) {
        $results['tests'][] = [
            'test' => 'Table Exists',
            'status' => 'success',
            'message' => 'Table tafsir_indonesian exists'
        ];
        
        // ===================================
        // 测试 5: 检查数据数量
        // ===================================
        $stmt = $pdo->query("SELECT COUNT(*) as total FROM tafsir_indonesian");
        $count = $stmt->fetch()['total'];
        
        $results['tests'][] = [
            'test' => 'Data Count',
            'status' => 'success',
            'result' => $count,
            'message' => "Found $count records in tafsir_indonesian"
        ];
        
        // ===================================
        // 测试 6: 查询示例数据
        // ===================================
        $stmt = $pdo->prepare("
            SELECT surah_id, ayat_id, language, SUBSTRING(text, 1, 100) as text_preview
            FROM tafsir_indonesian 
            WHERE surah_id = 1 AND ayat_id = 1 AND language = 'id'
            LIMIT 1
        ");
        $stmt->execute();
        $sampleData = $stmt->fetch();
        
        if ($sampleData) {
            $results['tests'][] = [
                'test' => 'Sample Data Query',
                'status' => 'success',
                'result' => $sampleData,
                'message' => 'Successfully retrieved sample data'
            ];
        } else {
            $results['tests'][] = [
                'test' => 'Sample Data Query',
                'status' => 'error',
                'message' => 'No data found for surah 1, ayah 1'
            ];
            $results['status'] = 'warning';
        }
        
    } else {
        $results['tests'][] = [
            'test' => 'Table Exists',
            'status' => 'error',
            'message' => 'Table tafsir_indonesian does not exist'
        ];
        $results['status'] = 'error';
    }
    
} catch (PDOException $e) {
    $results['tests'][] = [
        'test' => 'Database Connection',
        'status' => 'error',
        'message' => 'Database connection failed: ' . $e->getMessage(),
        'error_details' => [
            'host' => $DB_HOST,
            'database' => $DB_NAME,
            'user' => $DB_USER
        ]
    ];
    $results['status'] = 'error';
}

// ===================================
// 测试 7: URL Rewrite 测试
// ===================================
$results['tests'][] = [
    'test' => 'URL Rewrite Info',
    'status' => 'info',
    'result' => [
        'request_uri' => $_SERVER['REQUEST_URI'] ?? 'N/A',
        'script_name' => $_SERVER['SCRIPT_NAME'] ?? 'N/A',
        'htaccess_active' => file_exists(__DIR__ . '/.htaccess') ? 'yes' : 'no'
    ],
    'message' => 'Check if .htaccess is being processed'
];

// ===================================
// 输出结果
// ===================================
if ($results['status'] === 'testing') {
    $results['status'] = 'success';
}

http_response_code($results['status'] === 'error' ? 500 : 200);
echo json_encode($results, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
?>

