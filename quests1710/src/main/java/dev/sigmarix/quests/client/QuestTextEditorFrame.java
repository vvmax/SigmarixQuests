package dev.sigmarix.quests.client;
import dev.sigmarix.quests.*;
import java.awt.*;import java.awt.image.BufferedImage;import java.io.InputStream;
import javax.imageio.ImageIO;import javax.swing.*;import javax.swing.border.TitledBorder;
import net.minecraft.client.Minecraft;import net.minecraft.util.ResourceLocation;import net.minecraft.util.StatCollector;
/** The reference uses a separate desktop text editor, with native Swing controls. */
final class QuestTextEditorFrame extends JFrame {
    private final String questId,originalTitle,originalSubtitle,originalDescription;
    private final JTextField title,subtitle;private final JTextArea description;
    static void open(QuestCore.Quest quest){BufferedImage logo=null;try(InputStream stream=Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation("ftbquests:textures/logotransparent.png")).getInputStream()){logo=ImageIO.read(stream);}catch(java.io.IOException ignored){}final BufferedImage image=logo;SwingUtilities.invokeLater(()->new QuestTextEditorFrame(quest,image).requestFocus());}
    private static String tr(String key){return StatCollector.translateToLocal(key);}
    private String caption(String chapter,String quest){return QuestText.tr("FTB Quests Text Editor | ")+chapter+" | "+quest;}
    private QuestTextEditorFrame(QuestCore.Quest quest,BufferedImage logo){
        questId=quest.id;originalTitle=quest.title;originalSubtitle=quest.subtitle;originalDescription=quest.description;
        QuestCore.Chapter chapter=QuestPresentation.chapter(ClientProxy.snapshot.book,quest.chapter);setTitle(caption(chapter==null?"":chapter.title.isEmpty()?tr("ftbquests.unnamed"):chapter.title,QuestTaskLabels.quest(quest)));if(logo!=null)setIconImage(logo);setResizable(true);setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel panel=new JPanel();panel.setLayout(new BoxLayout(panel,BoxLayout.PAGE_AXIS));
        panel.add(title=new JTextField(originalTitle,75));panel.add(subtitle=new JTextField(originalSubtitle,75));panel.add(description=new JTextArea(originalDescription,30,75));
        title.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.GRAY,1,true),tr("ftbquests.title")));subtitle.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.GRAY,1,true),tr("ftbquests.quest.subtitle")));description.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.GRAY,1,true),tr("ftbquests.quest.description")));
        JPanel buttons=new JPanel();JButton reset=new JButton(QuestText.tr("Reset")),save=new JButton(QuestText.tr("Save"));buttons.add(reset);buttons.add(save);reset.addActionListener(event->{title.setText(originalTitle);subtitle.setText(originalSubtitle);description.setText(originalDescription);});
        save.addActionListener(event->{String nextTitle=title.getText().trim(),nextSubtitle=subtitle.getText().trim(),nextDescription=description.getText();Minecraft.getMinecraft().func_152344_a(()->{
            try{QuestCore.Book draft=QuestService.JSON.fromJson(QuestService.JSON.toJson(ClientProxy.snapshot.book),QuestCore.Book.class);QuestCore.Quest updated=null;for(QuestCore.Quest candidate:draft.quests)if(candidate.id.equals(questId)){updated=candidate;break;}if(updated==null)throw new IllegalArgumentException("Quest was deleted");updated.title=nextTitle;updated.subtitle=nextSubtitle;updated.description=String.join("\n",nextDescription.split("\n"));QuestObjectConfigScreen.send(draft,ClientProxy.snapshot.revision);QuestCore.Chapter owner=QuestPresentation.chapter(draft,updated.chapter);String caption=caption(owner==null?"":owner.title.isEmpty()?tr("ftbquests.unnamed"):owner.title,QuestTaskLabels.quest(updated));SwingUtilities.invokeLater(()->setTitle(caption));}catch(RuntimeException error){SwingUtilities.invokeLater(()->JOptionPane.showMessageDialog(this,QuestText.tr(error.getMessage()),"FTB Quests",JOptionPane.ERROR_MESSAGE));}
        });});panel.add(buttons);setContentPane(panel);pack();setLocationRelativeTo(null);setVisible(true);
    }
}
