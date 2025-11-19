#!/usr/bin/env python3
"""
自动上传 Tafsir 清单文件到服务器
"""

import json
import requests
import sys
from pathlib import Path

def create_tafsir_manifest():
    """创建 Tafsir 清单数据"""
    manifest = {
        "tafsirs": {
            "en": [
                {
                    "key": "en-tafisr-ibn-kathir",
                    "name": "Tafsir Ibn Kathir",
                    "author": "Ibn Kathir",
                    "langCode": "en",
                    "langName": "english",
                    "slug": "en-tafisr-ibn-kathir"
                }
            ],
            "id": [
                {
                    "key": "id-tafsir-kemenag",
                    "name": "Tafsir Al-Qur'an Kemenag",
                    "author": "Kementerian Agama Republik Indonesia",
                    "langCode": "id",
                    "langName": "indonesian",
                    "slug": "id-tafsir-kemenag"
                }
            ],
            "ar": [
                {
                    "key": "ar-tafsir-muyassar",
                    "name": "التفسير الميسر",
                    "author": "مجمع الملك فهد لطباعة المصحف الشريف",
                    "langCode": "ar",
                    "langName": "arabic",
                    "slug": "ar-tafsir-muyassar"
                }
            ]
        }
    }
    return manifest

def upload_to_server_direct():
    """尝试直接 HTTP PUT 上传到服务器"""
    print("🚀 方法 1: 尝试直接 HTTP PUT 上传...")
    print("=" * 60)
    
    manifest = create_tafsir_manifest()
    json_data = json.dumps(manifest, ensure_ascii=False, indent=2)
    
    target_url = "https://apis.dochubai.com/quran/apis/tafsirs/available_tafsirs_info.json"
    
    try:
        # 尝试 PUT 请求（如果服务器支持）
        headers = {
            'Content-Type': 'application/json; charset=utf-8',
            'User-Agent': 'Quran0-TafsirUploader/1.0'
        }
        
        response = requests.put(target_url, data=json_data.encode('utf-8'), headers=headers, timeout=10)
        
        if response.status_code in [200, 201, 204]:
            print(f"✅ 上传成功！(HTTP {response.status_code})")
            return True
        else:
            print(f"⚠️  上传失败 (HTTP {response.status_code})")
            print(f"响应: {response.text[:200]}")
            return False
            
    except Exception as e:
        print(f"❌ 直接上传失败: {e}")
        return False

def upload_to_admin_api():
    """尝试通过管理 API 上传"""
    print("\n🚀 方法 2: 尝试通过管理 API 上传...")
    print("=" * 60)
    
    manifest = create_tafsir_manifest()
    json_data = json.dumps(manifest, ensure_ascii=False, indent=2)
    
    # 尝试几个可能的管理 API 端点
    possible_endpoints = [
        "https://apis.dochubai.com/admin/upload/tafsir-manifest",
        "https://apis.dochubai.com/api/admin/files/upload",
        "https://dochubai.com/api/v1/tafsir/manifest",
    ]
    
    for endpoint in possible_endpoints:
        print(f"\n尝试: {endpoint}")
        try:
            files = {
                'file': ('available_tafsirs_info.json', json_data.encode('utf-8'), 'application/json')
            }
            
            response = requests.post(endpoint, files=files, timeout=10)
            
            if response.status_code in [200, 201, 204]:
                print(f"✅ 上传成功！(HTTP {response.status_code})")
                return True
            else:
                print(f"⚠️  此端点不可用 (HTTP {response.status_code})")
                
        except Exception as e:
            print(f"⚠️  此端点失败: {e}")
            continue
    
    return False

def save_to_assets_and_github():
    """保存到 assets 和 GitHub 仓库（让 CI/CD 自动部署）"""
    print("\n🚀 方法 3: 保存到本地并准备 Git 部署...")
    print("=" * 60)
    
    manifest = create_tafsir_manifest()
    json_data = json.dumps(manifest, ensure_ascii=False, indent=2)
    
    # 1. 更新 assets 文件
    assets_path = Path("app/src/main/assets/tafsir/available_tafsirs_info.json")
    assets_path.parent.mkdir(parents=True, exist_ok=True)
    assets_path.write_text(json_data, encoding='utf-8')
    print(f"✅ 已更新: {assets_path}")
    
    # 2. 创建服务器部署文件
    server_deploy_path = Path("server_deploy/available_tafsirs_info.json")
    server_deploy_path.parent.mkdir(parents=True, exist_ok=True)
    server_deploy_path.write_text(json_data, encoding='utf-8')
    print(f"✅ 已创建: {server_deploy_path}")
    
    # 3. 创建部署说明
    deploy_instructions = """
# 🚀 服务器部署说明

## 文件位置
本地文件: server_deploy/available_tafsirs_info.json
目标位置: https://apis.dochubai.com/quran/apis/tafsirs/available_tafsirs_info.json

## 自动部署（推荐）

如果您的服务器使用 Git 部署，请执行：

```bash
# 1. 提交到 Git
git add server_deploy/available_tafsirs_info.json
git add app/src/main/assets/tafsir/available_tafsirs_info.json
git commit -m "Add Tafsir manifest with Indonesian support"
git push origin main

# 2. 在服务器上部署
ssh user@dochubai.com << 'ENDSSH'
cd /var/www/apis.dochubai.com
git pull origin main
mkdir -p quran/apis/tafsirs
cp server_deploy/available_tafsirs_info.json quran/apis/tafsirs/
chmod 644 quran/apis/tafsirs/available_tafsirs_info.json
chown www-data:www-data quran/apis/tafsirs/available_tafsirs_info.json
ENDSSH
```

## 手动部署

如果需要手动上传，请：

1. 打开 FTP/SFTP 客户端
2. 连接到 dochubai.com
3. 上传文件到: /quran/apis/tafsirs/available_tafsirs_info.json
4. 设置权限: 644

## 验证部署

```bash
curl https://apis.dochubai.com/quran/apis/tafsirs/available_tafsirs_info.json
```

应该返回 JSON 内容。
"""
    
    deploy_readme = Path("server_deploy/DEPLOY.md")
    deploy_readme.write_text(deploy_instructions)
    print(f"✅ 已创建: {deploy_readme}")
    
    print("\n📋 下一步：")
    print("   1. git add server_deploy/ app/src/main/assets/tafsir/")
    print("   2. git commit -m 'Add Tafsir manifest'")
    print("   3. git push origin main")
    print("   4. 在服务器上执行部署（见 server_deploy/DEPLOY.md）")
    
    return True

def create_webhook_deploy():
    """创建 GitHub Actions 自动部署配置"""
    print("\n🚀 方法 4: 创建 GitHub Actions 自动部署...")
    print("=" * 60)
    
    github_action = """
name: Deploy Tafsir Manifest

on:
  push:
    branches: [ main ]
    paths:
      - 'server_deploy/available_tafsirs_info.json'

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Deploy to server
        env:
          SSH_PRIVATE_KEY: ${{ secrets.SERVER_SSH_KEY }}
          SERVER_HOST: ${{ secrets.SERVER_HOST }}
          SERVER_USER: ${{ secrets.SERVER_USER }}
        run: |
          mkdir -p ~/.ssh
          echo "$SSH_PRIVATE_KEY" > ~/.ssh/id_rsa
          chmod 600 ~/.ssh/id_rsa
          ssh-keyscan -H $SERVER_HOST >> ~/.ssh/known_hosts
          
          scp server_deploy/available_tafsirs_info.json \\
            ${SERVER_USER}@${SERVER_HOST}:/var/www/html/quran/apis/tafsirs/
          
          ssh ${SERVER_USER}@${SERVER_HOST} \\
            'chmod 644 /var/www/html/quran/apis/tafsirs/available_tafsirs_info.json'
          
          echo "✅ Deployment successful!"
      
      - name: Verify deployment
        run: |
          sleep 2
          curl -f https://apis.dochubai.com/quran/apis/tafsirs/available_tafsirs_info.json || exit 1
          echo "✅ File is accessible!"
"""
    
    github_workflow_path = Path(".github/workflows/deploy-tafsir.yml")
    github_workflow_path.parent.mkdir(parents=True, exist_ok=True)
    github_workflow_path.write_text(github_action)
    print(f"✅ 已创建 GitHub Actions 配置: {github_workflow_path}")
    
    secrets_guide = """
# GitHub Secrets 配置

在 GitHub 仓库设置中添加以下 Secrets：

1. SERVER_SSH_KEY - 服务器 SSH 私钥
2. SERVER_HOST - 服务器地址 (例如: dochubai.com)
3. SERVER_USER - SSH 用户名 (例如: ubuntu)

配置路径: GitHub 仓库 → Settings → Secrets and variables → Actions → New repository secret
"""
    
    secrets_path = Path(".github/SECRETS_GUIDE.md")
    secrets_path.write_text(secrets_guide)
    print(f"✅ 已创建 Secrets 配置说明: {secrets_path}")
    
    return True

def main():
    """主函数"""
    print("=" * 60)
    print("📤 Tafsir 清单自动上传工具")
    print("=" * 60)
    print()
    
    # 尝试方法 1: 直接 HTTP PUT
    if upload_to_server_direct():
        print("\n🎉 上传成功！")
        verify_deployment()
        return 0
    
    # 尝试方法 2: 管理 API
    if upload_to_admin_api():
        print("\n🎉 上传成功！")
        verify_deployment()
        return 0
    
    # 方法 3: 保存本地，准备 Git 部署
    print("\n⚠️  无法直接上传到服务器")
    print("正在准备替代方案...\n")
    
    save_to_assets_and_github()
    create_webhook_deploy()
    
    print("\n" + "=" * 60)
    print("📦 文件已准备完毕！")
    print("=" * 60)
    print("\n✅ 已完成：")
    print("   1. ✅ 更新 app/src/main/assets/tafsir/available_tafsirs_info.json")
    print("   2. ✅ 创建 server_deploy/available_tafsirs_info.json")
    print("   3. ✅ 创建 .github/workflows/deploy-tafsir.yml (CI/CD)")
    print("   4. ✅ 创建部署说明文档")
    
    print("\n🚀 现在可以：")
    print("\n   选项 A - 使用 GitHub Actions 自动部署（推荐）：")
    print("   -----------------------------------------")
    print("   1. 配置 GitHub Secrets（见 .github/SECRETS_GUIDE.md）")
    print("   2. git add . && git commit -m 'Add Tafsir manifest'")
    print("   3. git push origin main")
    print("   4. GitHub Actions 自动部署到服务器")
    print()
    print("   选项 B - 手动部署：")
    print("   -----------------------------------------")
    print("   见 server_deploy/DEPLOY.md 中的说明")
    print()
    print("   选项 C - 立即编译测试应用（使用内置 assets）：")
    print("   -----------------------------------------")
    print("   ./gradlew :app:assembleDebug")
    print("   adb install app/build/outputs/apk/debug/app-debug.apk")
    print()
    
    return 0

def verify_deployment():
    """验证部署结果"""
    print("\n🔍 验证部署...")
    print("=" * 60)
    
    target_url = "https://apis.dochubai.com/quran/apis/tafsirs/available_tafsirs_info.json"
    
    try:
        response = requests.get(target_url, timeout=10)
        if response.status_code == 200:
            print(f"✅ 文件可访问: {target_url}")
            data = response.json()
            lang_count = len(data.get('tafsirs', {}))
            print(f"✅ 包含 {lang_count} 种语言的 Tafsir")
            
            if 'id' in data.get('tafsirs', {}):
                print("✅ 印尼语 Tafsir 已包含")
            
            return True
        else:
            print(f"⚠️  文件不可访问 (HTTP {response.status_code})")
            return False
    except Exception as e:
        print(f"⚠️  验证失败: {e}")
        return False

if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print("\n\n⚠️  用户中断")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ 错误: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

