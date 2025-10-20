import React, { useState } from "react";
import { FaClock, FaMapMarkerAlt } from "react-icons/fa";
import ReservationModal from "./ReservationModal";
import { Table } from "@/types/FormData";
import { useSelector } from "react-redux";
import { RootState } from "@/redux/store";

interface TableListProps {
  tables: Table[];
  date: string;
}

const TableList: React.FC<TableListProps> = ({ tables, date }) => {
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [selectedTable, setSelectedTable] = useState<Table | null>(null);
  const [selectedSlot, setSelectedSlot] = useState("");
  const [guests, setGuests] = useState(1);

  const openModal = (table: Table, slot: string) => {
    setSelectedTable(table);
    setSelectedSlot(slot);
    setIsModalOpen(true);
  };

  const location = useSelector((state: RootState) =>
    state.locations.locations?.find((loc) => loc.id === tables[0]?.locationId)
  );

  return (
    <div className="mt-6 px-4">
      {tables.length > 0 && (
        <h3 className="text-xl font-semibold mb-4">
          {tables.length} tables available
        </h3>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {tables.map((table) => (
          <div
            key={table.id}
            className="bg-white p-4 shadow-md rounded-lg flex flex-col md:flex-row h-full"
          >
            <img
              src={location?.imageUrl}
              alt="Restaurant"
              className="w-full md:w-40 h-40 md:h-auto  object-cover rounded-lg"
            />
            <div className="mt-4 md:mt-0 md:ml-4 flex-1 ">
              <div className="flex items-center justify-between flex-wrap">
                <h4 className="font-semibold text-lg flex items-center text-wrap">
                  <FaMapMarkerAlt className="text-green-600 mr-2" />
                  {table.locationAddress}
                </h4>
                <p className="text-sm text-gray-700">
                  Table #{table.tableNumber}
                </p>
              </div>
              <p className="text-gray-600 mt-1">
                Table seating capacity: {table.guestCapacity} people
              </p>
              <p className="text-gray-700 font-medium mt-2">Available slots:</p>
              <div className="flex flex-wrap gap-2 mt-2">
                {table.availableSlots.map((slot, index) => (
                  <button
                    key={index}
                    className="flex items-center px-3 py-1 border border-green-600 text-green-600 rounded-lg hover:bg-green-100 text-sm"
                    onClick={() => openModal(table, slot)}
                  >
                    <FaClock className="mr-1" /> {slot}
                  </button>
                ))}
              </div>
            </div>
          </div>
        ))}
      </div>

      {selectedTable && (
        <ReservationModal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          location={selectedTable.locationAddress}
          date={date}
          guests={guests}
          setGuests={setGuests}
          timeSlot={selectedSlot}
          tableNumber={selectedTable.tableNumber}
          locationId={selectedTable.locationId}
          tableCap={selectedTable.guestCapacity}
        />
      )}
    </div>
  );
};

export default TableList;
