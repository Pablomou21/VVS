package es.udc.paproject.backend.rest.dtos;

import es.udc.paproject.backend.model.entities.Session;
import es.udc.paproject.backend.model.services.BillboardEntry;

import java.util.List;
import java.util.stream.Collectors;

public class BillboardEntryConversor {

    private BillboardEntryConversor() {}

    public final static List<BillboardEntryDto> toBillboardEntryDtos(List<BillboardEntry> billboard) {
        return billboard.stream().map(e -> toBillboardEntryDto(e)).collect(Collectors.toList());
    }

    public final static BillboardEntryDto toBillboardEntryDto(BillboardEntry billboardEntry) {
        List<BillboardSessionDto> billboardSessionDtos = toBillboardSessionDtos(billboardEntry.getSessions());
        return new BillboardEntryDto(billboardEntry.getMovie().getId(), billboardEntry.getMovie().getTitle(), billboardSessionDtos);
    }

    public final static BillboardSessionDto toBillboardSessionDto(Session session) {
        return new BillboardSessionDto(session.getId(), session.getDate().toString());
    }

    public final static List<BillboardSessionDto> toBillboardSessionDtos(List<Session> sessions) {
        return sessions.stream().map(s -> toBillboardSessionDto(s)).collect(Collectors.toList());
    }
}
