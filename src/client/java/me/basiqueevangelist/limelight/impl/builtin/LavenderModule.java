package me.basiqueevangelist.limelight.impl.builtin;

import io.wispforest.lavender.book.Book;
import io.wispforest.lavender.book.Entry;
import io.wispforest.lavender.book.LavenderBookItem;
import io.wispforest.lavender.client.LavenderBookScreen;
import me.basiqueevangelist.limelight.api.action.InvokeResultEntryAction;
import me.basiqueevangelist.limelight.api.action.ResultEntryAction;
import me.basiqueevangelist.limelight.api.entry.ResultEntry;
import me.basiqueevangelist.limelight.api.module.LimelightModule;
import me.basiqueevangelist.limelight.impl.Limelight;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class LavenderModule implements LimelightModule {
    public static final Identifier ID = Limelight.id("lavender");
    public static final LavenderModule INSTANCE = new LavenderModule();

    private LavenderModule() { }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void gatherEntries(String searchText, Consumer<ResultEntry> entryConsumer) {
        var player = MinecraftClient.getInstance().player;
        if (player == null) return;

        Map<Book, Integer> books = new HashMap<>();

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);

            var book = LavenderBookItem.bookOf(stack);
            if (book == null) continue;

            books.putIfAbsent(book, i);
        }

        for (var bookEntry : books.entrySet()) {
            for (var entry : bookEntry.getKey().entries()) {
                var result = new LavenderEntryEntry(bookEntry.getValue(), bookEntry.getKey(), entry);
                if (!StringUtils.containsIgnoreCase(result.text().getString(), searchText)) continue;
                entryConsumer.accept(result);
            }
        }
    }

    private record LavenderEntryEntry(int bookSlot, Book book, Entry entry) implements ResultEntry, InvokeResultEntryAction {

        @Override
        public LimelightModule module() {
            return INSTANCE;
        }

        @Override
        public String entryId() {
            return "limelight:lavender/" + entry.id().getNamespace() + "/" + entry.id().getPath();
        }

        @Override
        public Text text() {
            MutableText path = Text.empty();

            path
                .append(LavenderBookItem.itemOf(book).getName())
                .append(" > ");

            path.append(entry.title());

            return path;
        }

        @Override
        public ResultEntryAction action() {
            return this;
        }

        @Override
        public void run() {
            var player = MinecraftClient.getInstance().player;
            if (player == null) return;

            if (bookSlot == PlayerInventory.OFF_HAND_SLOT) {
                // nothing to do.
            } else if (PlayerInventory.isValidHotbarIndex(bookSlot)) {
                player.getInventory().selectedSlot = bookSlot;
            } else {
                player.getInventory().swapSlotWithHotbar(bookSlot);
            }

            LavenderBookScreen.pushEntry(book, entry);
            MinecraftClient.getInstance().setScreen(new LavenderBookScreen(book));
        }
    }
}
