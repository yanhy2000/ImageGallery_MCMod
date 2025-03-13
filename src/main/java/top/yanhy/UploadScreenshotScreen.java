package top.yanhy;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

import static top.yanhy.Screenshot_uploader.*;


public class UploadScreenshotScreen extends Screen {
    private static final Logger LOGGER = LogManager.getLogger("ScreenshotUploader");
    private final String filename;
    private TextFieldWidget descriptionField;
    private TextFieldWidget albumField;

    public UploadScreenshotScreen(String filename) {
        super(Text.literal("上传图片到图片墙"));
        this.filename = filename;
    }

    @Override
    protected void init() {
        this.descriptionField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, this.height / 2 - 50, 200, 20, Text.literal("图片描述"));
        this.descriptionField.setSuggestion("图片描述: 这个人很懒，什么都没写");
        this.descriptionField.setMaxLength(50);
        this.addSelectableChild(this.descriptionField);
        this.descriptionField.setFocusUnlocked(true);
        this.descriptionField.setChangedListener(text -> this.descriptionField.setSuggestion(null));

        this.albumField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, this.height / 2 - 20, 200, 20, Text.literal("相册集"));
        this.albumField.setSuggestion("相册: 留空默认使用用户名");
        this.albumField.setMaxLength(15);
        this.addSelectableChild(this.albumField);
        this.albumField.setFocusUnlocked(true);
        this.albumField.setChangedListener(text -> this.albumField.setSuggestion(null));

        ButtonWidget buttonUpload = ButtonWidget.builder(Text.of("上传"), (btn) -> {
            if (this.client != null) {
                handleUpload(this.filename, this.descriptionField.getText(), this.albumField.getText());
                this.client.getToastManager().add(
                        SystemToast.create(this.client, SystemToast.Type.NARRATOR_TOGGLE, Text.of("上传中..."), Text.of("开始上传图片墙"))
                );
            }
        }).dimensions(this.width / 2 - 110, this.height / 2 + 20, 100, 20).build();

        ButtonWidget buttonClose = ButtonWidget.builder(Text.of("取消"), (btn) -> {
            if (this.client != null) {
                close();
            }
        }).dimensions(this.width / 2 + 10, this.height / 2 + 20, 100, 20).build();
        this.addDrawableChild(buttonUpload);
        this.addDrawableChild(buttonClose);
    }
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.renderBackground(context,mouseX,mouseY,delta);
        context.drawText(this.textRenderer, this.title, this.width / 2-40, this.height / 2 - 80, 0xFFFFFFFF, true);
        super.render(context, mouseX, mouseY, delta);
        this.descriptionField.render(context, mouseX, mouseY, delta);
        this.albumField.render(context, mouseX, mouseY, delta);

    }

    private void handleUpload(String filename, String description, String album) {
        File mcDirectory = MinecraftClient.getInstance().runDirectory;
        String filepath = String.valueOf(new File(mcDirectory, "screenshots/" + filename).getAbsoluteFile());
        if (description.isEmpty()) {
            description = "这个人很懒，什么都没写";
        }
        LOGGER.info("上传截图: 文件路径={}, 描述={}, 相册={}", filepath, description, album);

        UploadHttpApi uploadHttpApi = Screenshot_uploader.getUploadHttpApi();
        // 调用异步上传方法
        uploadHttpApi.uploadImageAsync(USERTOKEN, filepath, filename, description, album, SERVERHOST, SERVERPORT, SERVERHTTP, new UploadHttpApi.UploadCallback() {
            @Override
            public void onSuccess(UploadHttpApi.UploadResponse uploadClass) {
                LOGGER.info("上传结果 Code={}, Message={}, Data={}", uploadClass.getCode(), uploadClass.getMessage(), uploadClass.getData());
                if (uploadClass.getCode() == 200) {
                    LOGGER.info("上传成功. Photo ID: {}", uploadClass.getData());

                    Text openWebUrlButton = Text.literal(" [打开图片墙网站] ")
                            .setStyle(Style.EMPTY
                                    .withColor(0x00FF00)
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, WEBURL))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("点击打开图片墙网站。")))
                            );
                    Text message = Text.literal(MOD_NAME + "上传成功!")
                            .append(openWebUrlButton);

                    MinecraftClient.getInstance().execute(() -> {
                        if (MinecraftClient.getInstance().player != null) {
                            MinecraftClient.getInstance().player.sendMessage(message, false);
                        }
                    });
                } else if (uploadClass.getCode() == 401) {
                    LOGGER.error("上传失败,Token失效. Message: {}", uploadClass.getMessage());
                    MinecraftClient.getInstance().execute(() -> {
                        if (MinecraftClient.getInstance().player != null) {
                            MinecraftClient.getInstance().player.sendMessage(Text.literal(MOD_NAME +"上传失败, 请检查用户Token信息！"), false);
                        }
                    });
                } else if (uploadClass.getCode() == 403) {
                    LOGGER.error("上传失败,账号无权限. Message: {}", uploadClass.getMessage());
                    MinecraftClient.getInstance().execute(() -> {
                        if (MinecraftClient.getInstance().player != null) {
                            MinecraftClient.getInstance().player.sendMessage(Text.literal(MOD_NAME +"上传失败, 账号无权限！"), false);
                        }
                    });
                } else if (uploadClass.getCode() == 500) {
                    LOGGER.error("上传失败,服务器响应错误. Message: {}", uploadClass.getMessage());
                    MinecraftClient.getInstance().execute(() -> {
                        if (MinecraftClient.getInstance().player != null) {
                            MinecraftClient.getInstance().player.sendMessage(Text.literal(MOD_NAME +"上传失败, 服务器响应错误！请检查服务器通讯是否正常。"), false);
                        }
                    });
                } else {
                    LOGGER.error("上传失败. Message: {}", uploadClass.getMessage());
                    MinecraftClient.getInstance().execute(() -> {
                        if (MinecraftClient.getInstance().player != null) {
                            MinecraftClient.getInstance().player.sendMessage(Text.literal(MOD_NAME +"上传失败！"), false);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                LOGGER.error("上传时发生异常. Message: {}", e.getMessage());
                MinecraftClient.getInstance().execute(() -> {
                    if (MinecraftClient.getInstance().player != null) {
                        MinecraftClient.getInstance().player.sendMessage(Text.literal(MOD_NAME +"上传未成功."), false);
                    }
                });
            }
        });

        this.close();
    }
    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(null);
    }
}
