package com.nesting.assistant.rag.loader;

import com.nesting.assistant.rag.service.NestingDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 初始化知识库加载器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InitialKnowledgeLoader implements ApplicationRunner {

    private final NestingDocumentService documentService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Loading initial knowledge base...");
//        loadErrorCodes();
//        loadFAQs();
//        loadBestPractices();
        log.info("Initial knowledge base loaded successfully");
    }

    private void loadErrorCodes() {
        // 常见套料软件错误代码
        documentService.ingestErrorCode(
                "NEST-001",
                "内存不足，无法完成套料计算",
                "1. 减少同时处理的零件数量\n2. 增加系统内存\n3. 使用分批套料模式\n4. 检查是否有内存泄漏",
                "NestingEngine",
                "error"
        );

        documentService.ingestErrorCode(
                "NEST-002",
                "许可证验证失败",
                "1. 检查许可证文件是否存在\n2. 确认许可证未过期\n3. 检查许可证绑定的机器码\n4. 联系供应商获取新许可证",
                "License",
                "critical"
        );

        documentService.ingestErrorCode(
                "NEST-003",
                "数据库连接失败",
                "1. 检查数据库服务是否启动\n2. 验证数据库连接参数\n3. 检查网络连接\n4. 确认数据库用户权限",
                "Database",
                "critical"
        );

        documentService.ingestErrorCode(
                "NEST-004",
                "零件文件导入失败",
                "1. 检查文件格式是否支持\n2. 验证文件是否损坏\n3. 检查文件路径是否包含特殊字符\n4. 尝试重新导出文件",
                "Import",
                "error"
        );

        documentService.ingestErrorCode(
                "NEST-005",
                "套料参数配置无效",
                "1. 检查间距参数是否为正值\n2. 验证板材尺寸是否合理\n3. 确认旋转角度设置\n4. 恢复默认配置后重试",
                "Config",
                "warning"
        );

        documentService.ingestErrorCode(
                "NEST-006",
                "切割路径生成失败",
                "1. 检查零件轮廓是否闭合\n2. 验证零件尺寸是否在板材范围内\n3. 检查切割参数设置\n4. 尝试简化复杂轮廓",
                "Cutting",
                "error"
        );

        log.info("Loaded {} error codes", 6);
    }

    private void loadFAQs() {
        documentService.ingestFAQ(
                "套料利用率不高怎么办？",
                """
                提高套料利用率的方法：
                1. 调整零件间距参数，适当减小间距（建议0.5-2mm）
                2. 开启零件旋转功能，增加摆放角度选项（如90度、180度）
                3. 使用共边切割功能，减少切割缝隙
                4. 优化零件排序策略，优先放置大件
                5. 使用余料套料功能，充分利用剩余板材
                6. 考虑混合套料，将不同项目的零件组合套料
                """,
                List.of("utilization", "optimization", "parameters"),
                "optimization"
        );

        documentService.ingestFAQ(
                "如何设置套料参数？",
                """
                套料参数设置建议：
                1. 零件间距：根据切割工艺设置，激光切割建议0.5-1mm，等离子切割建议2-3mm
                2. 板材边距：通常设置为切割头半径+安全距离，建议5-10mm
                3. 旋转角度：支持0度、90度、180度、270度，开启更多角度可提高利用率但增加计算时间
                4. 优先级：设置零件优先级，重要零件优先套料
                5. 共边切割：相邻零件共享切割边，提高利用率但需要特定切割工艺支持
                """,
                List.of("parameters", "config", "setup"),
                "configuration"
        );

        documentService.ingestFAQ(
                "软件运行缓慢怎么办？",
                """
                性能优化建议：
                1. 减少同时处理的零件数量，分批处理
                2. 降低套料精度要求（如从"高精度"改为"标准"）
                3. 关闭不必要的实时预览功能
                4. 增加JVM内存配置（修改启动参数-Xmx）
                5. 检查磁盘空间和读写速度
                6. 定期清理历史数据和临时文件
                """,
                List.of("performance", "slow", "optimization"),
                "troubleshooting"
        );

        documentService.ingestFAQ(
                "如何备份和恢复配置？",
                """
                配置备份与恢复：
                1. 备份位置：系统设置 -> 配置管理 -> 导出配置
                2. 备份内容包括：套料参数、材料库、零件库、用户偏好设置
                3. 配置文件格式为JSON，可手动编辑
                4. 恢复配置：系统设置 -> 配置管理 -> 导入配置
                5. 建议定期备份，特别是在软件升级前
                """,
                List.of("backup", "config", "restore"),
                "configuration"
        );

        documentService.ingestFAQ(
                "支持哪些文件格式导入？",
                """
                支持的文件格式：
                1. CAD格式：DXF、DWG（AutoCAD 2007及以上版本）
                2. 通用格式：SVG、HPGL、NC
                3. 三维格式：STEP、IGES（需要转换为二维轮廓）
                4. 数据库格式：支持从数据库直接导入零件信息
                5. Excel格式：支持批量导入零件清单
                """,
                List.of("import", "format", "file"),
                "import"
        );

        log.info("Loaded {} FAQs", 5);
    }

    private void loadBestPractices() {
        documentService.ingestBestPractice(
                "板材套料最佳实践",
                """
                板材套料最佳实践指南：

                1. 材料准备
                - 确认板材规格和材质
                - 检查板材表面质量
                - 记录板材编号便于追溯

                2. 零件准备
                - 检查零件轮廓完整性
                - 设置零件切割顺序
                - 标记重点零件

                3. 参数设置
                - 根据材料厚度调整间距
                - 设置合适的切割补偿
                - 配置引弧点和穿孔点

                4. 套料执行
                - 先手动放置大件和异形件
                - 再自动套料小件
                - 最后微调优化

                5. 质量检查
                - 检查套料覆盖率
                - 验证切割路径
                - 导出NC代码进行模拟
                """,
                "板材套料",
                List.of("best-practice", "sheet-metal", "workflow")
        );

        documentService.ingestBestPractice(
                "提高套料效率的技巧",
                """
                提高套料效率的技巧：

                1. 零件分类管理
                - 按材质、厚度分类存储
                - 建立常用零件库
                - 设置零件标签便于检索

                2. 模板使用
                - 保存常用套料模板
                - 复制历史成功套料方案
                - 建立标准参数模板

                3. 批量处理
                - 使用批处理功能处理相似项目
                - 设置自动套料规则
                - 利用空闲时间进行后台套料

                4. 系统维护
                - 定期清理临时文件
                - 优化数据库性能
                - 保持软件版本更新
                """,
                "效率优化",
                List.of("efficiency", "workflow", "tips")
        );

        log.info("Loaded {} best practices", 2);
    }
}
