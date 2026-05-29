# 快速清理旧问卷并重启应用的脚本

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  清理旧问卷并重启应用" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# 提示用户停止当前应用
Write-Host "请先按 Ctrl+C 停止当前运行的应用..." -ForegroundColor Yellow
Write-Host "按任意键继续..." -ForegroundColor Yellow
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

Write-Host ""
Write-Host "1. 清理数据库中的旧问卷数据..." -ForegroundColor Green
Write-Host "   请在 MySQL 中执行以下命令：" -ForegroundColor Gray
Write-Host "   source D:\Users\Desktop\yuwen\yuwen\magic-cube\cleanup_old_questionnaire.sql" -ForegroundColor Cyan
Write-Host ""

# 提示用户执行 SQL
Write-Host "是否已在 MySQL 中执行清理 SQL？(Y/N)" -ForegroundColor Yellow
$response = Read-Host

if ($response -eq "Y" -or $response -eq "y") {
    Write-Host ""
    Write-Host "2. 启动应用..." -ForegroundColor Green
    .\mvnw.cmd spring-boot:run
} else {
    Write-Host ""
    Write-Host "请先执行清理 SQL，然后手动运行：.\mvnw.cmd spring-boot:run" -ForegroundColor Yellow
}
