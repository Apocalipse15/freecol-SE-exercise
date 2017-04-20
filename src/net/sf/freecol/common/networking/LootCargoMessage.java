/**
 *  Copyright (C) 2002-2017   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.networking;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;

import net.sf.freecol.common.util.DOMUtils;
import org.w3c.dom.Element;


/**
 * The message sent when looting cargo.
 */
public class LootCargoMessage extends ObjectMessage {

    public static final String TAG = "lootCargo";
    private static final String LOSER_TAG = "loser";
    private static final String WINNER_TAG = "winner";

    /** The goods to be looted. */
    private List<Goods> goods = new ArrayList<>();


    /**
     * Create a new {@code LootCargoMessage}.
     *
     * @param winner The {@code Unit} that is looting.
     * @param loserId The identifier of the {@code Unit} that is looted.
     * @param goods The {@code AbstractGoods} to loot.
     */
    public LootCargoMessage(Unit winner, String loserId, List<Goods> goods) {
        super(TAG, WINNER_TAG, winner.getId(), LOSER_TAG, loserId);

        this.goods.clear();
    }

    /**
     * Create a new {@code LootCargoMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public LootCargoMessage(Game game, Element element) {
        super(TAG, WINNER_TAG, getStringAttribute(element, WINNER_TAG),
              LOSER_TAG, getStringAttribute(element, LOSER_TAG));

        this.goods.addAll(DOMUtils.getChildren(game, element, Goods.class));
    }

    /**
     * Create a new {@code LootCargoMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public LootCargoMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, WINNER_TAG, LOSER_TAG);

        this.goods.clear();
        while (xr.moreTags()) {
            String tag = xr.getLocalName();
            if (Goods.TAG.equals(tag)) {
                if (this.goods == null) {
                    Goods g = xr.readFreeColObject(game, Goods.class);
                    if (g != null) this.goods.add(g);
                } else {
                    expected(TAG, tag);
                }
            } else {
                expected(Goods.TAG, tag);
            }
            xr.expectTag(tag);
        }
        xr.expectTag(TAG);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return Message.MessagePriority.LATE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void aiHandler(FreeColServer freeColServer, AIPlayer aiPlayer) {
        final Game game = freeColServer.getGame();
        final Unit winner = getWinner(game);
        final List<Goods> initialGoods = getGoods();
        final String loserId = getLoserId();

        aiPlayer.lootCargoHandler(winner, initialGoods, loserId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clientHandler(FreeColClient freeColClient) {
        final Game game = freeColClient.getGame();
        final Unit unit = getWinner(game);
        final String loserId = getLoserId();
        final List<Goods> goods = getGoods();

        if (unit == null || goods == null) return;

        igc(freeColClient).lootCargoHandler(unit, goods, loserId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final Game game = freeColServer.getGame();

        Unit winner;
        try {
            winner = getWinner(game);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }
        // Do not check the defender identifier, as it might have
        // sunk.  It is enough that the attacker knows it.  Similarly
        // the server is better placed to check the goods validity.

        // Try to loot.
        return igc(freeColServer)
            .lootCargo(serverPlayer, winner, getLoserId(), getGoods());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        for (Goods g : this.goods) g.toXML(xw);
    }

    /**
     * Convert this LootCargoMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(TAG,
            WINNER_TAG, getStringAttribute(WINNER_TAG),
            LOSER_TAG, getLoserId())
            .add(getGoods()).toXMLElement();
    }

    // Public interface

    /**
     * Public accessor to help the client in game controller.
     *
     * @param game The {@code Game} to look for the unit in.
     * @return The winner unit.
     */
    public Unit getWinner(Game game) {
        return game.getFreeColGameObject(getStringAttribute(WINNER_TAG),
                                         Unit.class);
    }

    /**
     * Public accessor to help the client in game controller.
     *
     * @return The loser unit object Identifier.
     */
    public String getLoserId() {
        return getStringAttribute(LOSER_TAG);
    }

    /**
     * Public accessor to help the client in game controller.
     *
     * @return The goods to loot.
     */
    public List<Goods> getGoods() {
        return this.goods;
    }
}
