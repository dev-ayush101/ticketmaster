import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';

export default function Home() {
  const [events, setEvents] = useState([]);
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const fetchEvents = async (search = '') => {
    setLoading(true);
    try {
      const params = search ? { keyword: search } : {};
      const res = await api.get('/events/search', { params });
      setEvents(res.data.content);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchEvents(); }, []);

  const handleSearch = (e) => {
    e.preventDefault();
    fetchEvents(keyword);
  };

  return (
    <div className="max-w-5xl mx-auto px-6 py-8">
      <h1 className="text-2xl font-bold mb-6 text-gray-800">Upcoming Events</h1>

      <form onSubmit={handleSearch} className="flex gap-3 mb-8">
        <input
          type="text"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="Search by event, artist, venue..."
          className="flex-1 px-4 py-2 rounded border border-gray-300 focus:outline-none focus:border-[#dc3558] text-sm"
        />
        <button type="submit" className="px-6 py-2 bg-[#dc3558] text-white rounded hover:bg-[#c22d4e] text-sm font-medium">
          Search
        </button>
      </form>

      {loading ? (
        <p className="text-gray-500">Loading...</p>
      ) : events.length === 0 ? (
        <p className="text-gray-500">No events found.</p>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {events.map(event => (
            <div
              key={event.id}
              onClick={() => navigate(`/event/${event.id}`)}
              className="border border-gray-200 rounded-lg overflow-hidden cursor-pointer hover:shadow-md transition"
            >
              <div className="bg-[#fafafa] h-32 flex items-center justify-center border-b border-gray-200">
                <svg xmlns="http://www.w3.org/2000/svg" className="w-10 h-10 text-[#dc3558]" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M12 3v10.55A4 4 0 1 0 14 17V7h4V3h-6Z"/>
                </svg>
              </div>
              <div className="p-4">
                <h3 className="font-semibold text-gray-900 mb-1">{event.name}</h3>
                <p className="text-sm text-[#dc3558] font-medium">{event.performer?.name}</p>
                <p className="text-xs text-gray-500 mt-1">{event.venue?.name}</p>
                <p className="text-xs text-gray-400 mt-2">
                  {new Date(event.eventDate).toLocaleDateString('en-US', {
                    weekday: 'short', year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
                  })}
                </p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}