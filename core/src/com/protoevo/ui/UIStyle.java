package com.protoevo.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.protoevo.utils.CursorUtils;

import java.time.format.TextStyle;

public class UIStyle {

    public static Texture getWhite1x1() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    public static BitmapFont createFiraCode(int size) {
        String fontPath = "fonts/FiraCode-Retina.ttf";
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.local(fontPath));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = size;
        parameter.borderWidth = size / 10f;
        parameter.borderColor = new Color(0, 0, 0, .5f);
        parameter.color = Color.WHITE;
        return generator.generateFont(parameter);
    }

    public static Skin getUISkin() {
        Skin skin = new Skin();

        skin.add("white", getWhite1x1());

        Color transparent = new Color(0, 0, 0, 0);
        Color selectionColor = new Color(0, 0, 0.8f, 0.25f);

        float graphicsHeight = Gdx.graphics.getHeight();
        int infoTextSize = (int) (graphicsHeight / 50f);
        // Store the default libGDX font under the name "default".
        skin.add("default", createFiraCode(infoTextSize));
        BitmapFont debugFont = createFiraCode(infoTextSize);
        debugFont.setColor(Color.GOLD);
        skin.add("debug", debugFont);

        BitmapFont statsTitleFont = UIStyle.createFiraCode((int) (graphicsHeight / 40f));
        skin.add("statsTitle", statsTitleFont);

        skin.add("mainTitle", UIStyle.createFiraCode((int) (graphicsHeight / 20f)));

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("default");
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        Label.LabelStyle debugLabelStyle = new Label.LabelStyle();
        debugLabelStyle.font = skin.getFont("debug");
        debugLabelStyle.fontColor = Color.GOLD;
        skin.add("debug", debugLabelStyle);

        Label.LabelStyle statsTitleLabelStyle = new Label.LabelStyle();
        statsTitleLabelStyle.font = skin.getFont("statsTitle");
        statsTitleLabelStyle.fontColor = Color.WHITE;
        skin.add("statsTitle", statsTitleLabelStyle);

        Label.LabelStyle mainTitleLabelStyle = new Label.LabelStyle();
        mainTitleLabelStyle.font = skin.getFont("mainTitle");
        mainTitleLabelStyle.fontColor = Color.WHITE;
        skin.add("mainTitle", mainTitleLabelStyle);

        // Configure a TextButtonStyle and name it "default". Skin resources are stored by type, so this doesn't overwrite the font.
        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.up = skin.newDrawable("white", transparent);
        textButtonStyle.down = skin.newDrawable("white", transparent);
        textButtonStyle.checked = skin.newDrawable("white", transparent);
        textButtonStyle.over = skin.newDrawable("white", selectionColor);
        textButtonStyle.font = skin.getFont("default");
        skin.add("default", textButtonStyle);

        SelectBox.SelectBoxStyle selectBoxStyle = new SelectBox.SelectBoxStyle();
        selectBoxStyle.font = skin.getFont("default");
        selectBoxStyle.fontColor = Color.WHITE;
        selectBoxStyle.background = skin.newDrawable("white", transparent);
        selectBoxStyle.scrollStyle = new ScrollPane.ScrollPaneStyle();
        selectBoxStyle.scrollStyle.background = skin.newDrawable("white", new Color(0, 0, 0, 0.95f));
        selectBoxStyle.listStyle = new List.ListStyle();
        selectBoxStyle.listStyle.font = skin.getFont("default");
        selectBoxStyle.listStyle.fontColorSelected = Color.WHITE;
        selectBoxStyle.listStyle.fontColorUnselected = new Color(0.7f, 0.7f, 0.7f, 1);
        selectBoxStyle.listStyle.selection = skin.newDrawable("white",  selectionColor);
        skin.add("default", selectBoxStyle);

        SelectBox.SelectBoxStyle titleSelectBoxStyle = new SelectBox.SelectBoxStyle(selectBoxStyle);
        titleSelectBoxStyle.font = skin.getFont("statsTitle");
        skin.add("statsTitle", titleSelectBoxStyle);

        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = skin.getFont("default");
        textFieldStyle.fontColor = Color.WHITE;
        textFieldStyle.background = skin.newDrawable("white", new Color(0.3f, 0.3f, 0.3f, 0.5f));
        textFieldStyle.disabledFontColor = Color.GRAY;
        textFieldStyle.cursor = skin.newDrawable("white", Color.WHITE);
        textFieldStyle.selection = skin.newDrawable("white", selectionColor);
        textFieldStyle.background.setLeftWidth(textFieldStyle.background.getLeftWidth() + 10);
        textFieldStyle.background.setRightWidth(textFieldStyle.background.getRightWidth() + 10);

        skin.add("default", textFieldStyle);

        return skin;
    }
}
