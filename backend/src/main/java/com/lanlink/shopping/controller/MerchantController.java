package com.lanlink.shopping.controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lanlink.shopping.common.R;
import com.lanlink.shopping.config.UserContext;
import com.lanlink.shopping.dto.MerchantApplyDTO;
import com.lanlink.shopping.dto.MerchantStatusDTO;
import com.lanlink.shopping.dto.MerchantUpdateDTO;
import com.lanlink.shopping.entity.Merchant;
import com.lanlink.shopping.service.MerchantService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * 商户入驻与筛选 + 运营审核
 */
@RestController
@RequestMapping("/merchant")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    /** 营业执照上传目录：{user.dir}/uploads/licenses（WebConfig 已映射 /uploads/**） */
    private static final String LICENSE_DIR = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "licenses";
    /** 税务证明上传目录 */
    private static final String TAX_PROOF_DIR = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "tax-proofs";
    private static final long MAX_SIZE = 5 * 1024 * 1024;
    /** 入驻申请材料上传上限：10MB（工商信息/纳税记录） */
    private static final long APPLY_MAX_SIZE = 10 * 1024 * 1024;
    /** 营业执照最小边长（像素）：保证执照信息完整可辨 */
    private static final int MIN_LICENSE_EDGE = 400;

    /** 提交入驻申请(自动执行筛选机制) */
    @PostMapping("/apply")
    public R<Merchant> apply(@Valid @RequestBody MerchantApplyDTO dto, HttpServletRequest request) {
        Long userId = UserContext.currentUserId(request);
        return R.ok("入驻申请已提交", merchantService.apply(userId, null, dto));
    }

    /**
     * 入驻申请材料上传（申请阶段专用，仅需登录）：工商信息/纳税记录在提交申请前先行上传，
     * 返回 { url } 随申请表单一并提交。与已入驻商户的 /license、/tax-proof（merchant:manage）权限隔离。
     */
    @PostMapping("/apply-upload")
    public R<Map<String, String>> applyUpload(@RequestParam("file") MultipartFile file,
                                              @RequestParam String kind,
                                              HttpServletRequest request) throws IOException {
        Long userId = UserContext.currentUserId(request);
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("请选择文件");
        if (file.getSize() > APPLY_MAX_SIZE) throw new IllegalArgumentException("文件大小不能超过 10MB");
        String detected = sniff(file);
        boolean license = "license".equals(kind);
        if (license && !"jpg".equals(detected) && !"png".equals(detected)) {
            throw new IllegalArgumentException("工商信息（营业执照）仅支持 JPG/PNG 格式");
        }
        if (!license && !"pdf".equals(detected) && !"jpg".equals(detected) && !"png".equals(detected)) {
            throw new IllegalArgumentException("纳税记录仅支持 JPG/PNG/PDF 格式");
        }
        String url = save(file, license ? LICENSE_DIR : TAX_PROOF_DIR,
                license ? "licenses" : "tax-proofs", userId, detected);
        return R.ok("上传成功", Map.of("url", url));
    }

    /** 我的商户信息（税务登记号脱敏展示） */
    @GetMapping("/my")
    public R<Merchant> my(HttpServletRequest request) {
        return R.ok(merchantService.getForDisplay(UserContext.currentUserId(request)));
    }

    /** 营业执照上传（商户专属）：JPG/PNG 合规校验，落盘并同步写入商户档案 license_url */
    @PostMapping("/license")
    @com.lanlink.shopping.integration.security.RequirePerm("merchant:manage")
    public R<Map<String, String>> uploadLicense(@RequestParam("file") MultipartFile file,
                                                HttpServletRequest request) throws IOException {
        Long userId = UserContext.currentUserId(request);
        String url = saveImage(file, LICENSE_DIR, "licenses", userId);
        merchantService.persistLicense(userId, url);
        return R.ok("营业执照已更新", Map.of("url", url));
    }

    /** 税务缴纳证明上传（商户专属）：PDF/JPG/PNG，落盘并追加写入商户档案 tax_proof_urls（最多 3 份） */
    @PostMapping("/tax-proof")
    @com.lanlink.shopping.integration.security.RequirePerm("merchant:manage")
    public R<Map<String, String>> uploadTaxProof(@RequestParam("file") MultipartFile file,
                                                 HttpServletRequest request) throws IOException {
        Long userId = UserContext.currentUserId(request);
        String url = saveTaxProof(file, TAX_PROOF_DIR, "tax-proofs", userId);
        merchantService.persistTaxProof(userId, url);
        return R.ok("税务证明已上传", Map.of("url", url));
    }

    /** 税务登记号查询（商户专属）：返回近 3 个月缴纳记录，并将登记号绑定至商户档案 */
    @GetMapping("/tax-query")
    @com.lanlink.shopping.integration.security.RequirePerm("merchant:manage")
    public R<Map<String, Object>> taxQuery(@RequestParam String taxRegNo, HttpServletRequest request) {
        Map<String, Object> out = merchantService.queryTaxRecords(taxRegNo);
        merchantService.persistTaxRegNo(UserContext.currentUserId(request), taxRegNo);
        return R.ok(out);
    }

    /** 运营端: 商户审核列表 (需 admin 角色, 由拦截器鉴权) */
    @GetMapping("/admin/list")
    public R<List<Merchant>> adminList(@RequestParam(required = false) Integer reviewStatus) {
        return R.ok(merchantService.listForAdmin(reviewStatus));
    }

    /** 运营端: 商户分页列表（支持审核状态/账户状态/关键词筛选，商户ID或企业ID精确、企业名称模糊） */
    @GetMapping("/admin/page")
    public R<Page<Merchant>> adminPage(@RequestParam(defaultValue = "1") long page,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) Integer reviewStatus,
                                       @RequestParam(required = false) Integer status,
                                       @RequestParam(required = false) String keyword) {
        return R.ok(merchantService.listForAdminPage(page, size, reviewStatus, status, keyword));
    }

    /** 入驻流程跟踪：某商户的审核历史 */
    @GetMapping("/admin/review-logs/{merId}")
    public R<List<com.lanlink.shopping.entity.MerchantReviewLog>> reviewLogs(@PathVariable Long merId) {
        return R.ok(merchantService.reviewLogs(merId));
    }

    /** 运营端: 商户详情（工商信息/资质/证照完整度，资料维护与资质验证视图） */
    @GetMapping("/admin/detail/{merId}")
    public R<Map<String, Object>> adminDetail(@PathVariable Long merId) {
        return R.ok(merchantService.adminDetail(merId));
    }

    /** 运营端: 审核通过/驳回 */
    @PostMapping("/admin/review/{merId}")
    public R<Merchant> review(@PathVariable Long merId,
                              @RequestParam Integer reviewStatus,
                              @RequestParam(required = false) String reason,
                              HttpServletRequest request) {
        return R.ok(merchantService.review(merId, reviewStatus, reason, UserContext.currentUserId(request)));
    }

    // ===== 运营端商户管理：基本资料维护 / 账户状态 / 权限配置（/admin/ 路径由 AuthInterceptor 限定仅平台运营）=====

    /** 运营端: 修改商户基本资料（企业工商信息联动更新，敏感字段加密存储） */
    @PutMapping("/admin/{merId}")
    public R<Merchant> adminUpdate(@PathVariable Long merId,
                                  @Valid @RequestBody MerchantUpdateDTO dto,
                                  HttpServletRequest request) {
        return R.ok("商户资料已更新", merchantService.updateByAdmin(merId, dto, UserContext.currentUserId(request), request));
    }

    /** 运营端: 注销商户（账户状态置注销 + 逻辑删除 + 商户身份降级） */
    @DeleteMapping("/admin/{merId}")
    public R<Void> adminDelete(@PathVariable Long merId,
                               @RequestParam(required = false) String reason,
                               HttpServletRequest request) {
        merchantService.deleteByAdmin(merId, reason, UserContext.currentUserId(request), request);
        return R.ok("商户已注销", null);
    }

    /** 运营端: 账户状态变更（1正常 2冻结 3注销，必须填写原因） */
    @PostMapping("/admin/status/{merId}")
    public R<Merchant> adminStatus(@PathVariable Long merId,
                                   @Valid @RequestBody MerchantStatusDTO dto,
                                   HttpServletRequest request) {
        return R.ok("账户状态已更新",
                merchantService.changeStatus(merId, dto.getStatus(), dto.getReason(), UserContext.currentUserId(request), request));
    }

    /** 运营端: 商户权限配置详情（可选权限白名单 + 当前授权） */
    @GetMapping("/admin/perms/{merId}")
    public R<Map<String, Object>> adminPermConfig(@PathVariable Long merId) {
        return R.ok(merchantService.permConfig(merId));
    }

    /** 运营端: 商户权限配置（授予/回收商户档可选权限，白名单外权限点拒绝） */
    @PutMapping("/admin/perms/{merId}")
    public R<Merchant> adminPermUpdate(@PathVariable Long merId,
                                       @RequestBody(required = false) List<String> permCodes,
                                       HttpServletRequest request) {
        return R.ok("权限配置已生效",
                merchantService.configPerms(merId, permCodes, UserContext.currentUserId(request), request));
    }

    /** 运营端: 解密查看完整税务登记号（敏感操作，写审计日志） */
    @GetMapping("/admin/{merId}/tax-reg-no")
    public R<Map<String, Object>> adminTaxRegNo(@PathVariable Long merId, HttpServletRequest request) {
        return R.ok(merchantService.viewTaxRegNo(merId, UserContext.currentUserId(request), request));
    }

    /** 驳回后重新提交入驻申请（复用原记录重新筛选，需登录） */
    @PostMapping("/reapply")
    public R<Merchant> reapply(@Valid @RequestBody MerchantApplyDTO dto, HttpServletRequest request) {
        Long userId = UserContext.currentUserId(request);
        return R.ok("重新提交成功", merchantService.reapply(userId, dto));
    }

    // ===== 上传文件落盘与合规校验 =====

    /** 读取文件头魔数，防伪造扩展名 */
    private static String sniff(MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream()) {
            byte[] h = in.readNBytes(8);
            if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) return "jpg";
            if (h.length >= 4 && (h[0] & 0xFF) == 0x89 && h[1] == 0x50 && h[2] == 0x4E && h[3] == 0x47) return "png";
            if (h.length >= 4 && h[0] == 0x25 && h[1] == 0x50 && h[2] == 0x44 && h[3] == 0x46) return "pdf"; // %PDF
            return "unknown";
        }
    }

    private static void checkCommon(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("请选择文件");
        if (file.getSize() > MAX_SIZE) throw new IllegalArgumentException("文件大小不能超过 5MB");
    }

    /** 营业执照：仅 JPG/PNG + 最小边长校验（信息完整可辨） */
    private String saveImage(MultipartFile file, String dir, String folder, Long userId) throws IOException {
        checkCommon(file);
        String kind = sniff(file);
        if (!"jpg".equals(kind) && !"png".equals(kind)) {
            throw new IllegalArgumentException("营业执照仅支持 JPG/PNG 格式图片");
        }
        BufferedImage img;
        try {
            img = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            throw new IllegalArgumentException("图片解析失败，请重新上传");
        }
        if (img == null || img.getWidth() < MIN_LICENSE_EDGE || img.getHeight() < MIN_LICENSE_EDGE) {
            throw new IllegalArgumentException("图片尺寸过小（需≥" + MIN_LICENSE_EDGE + "×" + MIN_LICENSE_EDGE
                    + "），请上传完整清晰的营业执照");
        }
        return save(file, dir, folder, userId, kind);
    }

    /** 税务证明：PDF/JPG/PNG */
    private String saveTaxProof(MultipartFile file, String dir, String folder, Long userId) throws IOException {
        checkCommon(file);
        String kind = sniff(file);
        if (!"pdf".equals(kind) && !"jpg".equals(kind) && !"png".equals(kind)) {
            throw new IllegalArgumentException("税务证明仅支持 PDF/JPG/PNG 格式");
        }
        return save(file, dir, folder, userId, kind);
    }

    private String save(MultipartFile file, String dir, String folder, Long userId, String ext) throws IOException {
        File d = new File(dir);
        if (!d.exists() && !d.mkdirs()) throw new RuntimeException("上传目录创建失败");
        String name = userId + "_" + System.currentTimeMillis() + "." + ext;
        File dest = new File(d, name);
        file.transferTo(dest.getAbsoluteFile());
        return "/api/uploads/" + folder + "/" + name;
    }
}
